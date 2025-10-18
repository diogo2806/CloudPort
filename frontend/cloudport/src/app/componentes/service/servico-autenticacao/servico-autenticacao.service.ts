import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { User } from '../../model/user.model';
import { ConfiguracaoAplicacaoService } from '../../../configuracao/configuracao-aplicacao.service';

@Injectable({ providedIn: 'root' })
export class ServicoAutenticacao {
    private readonly usuarioAtual$: BehaviorSubject<User | null>;
    public readonly usuarioAtualObservavel: Observable<User | null>;
    private readonly statusMenu$: BehaviorSubject<boolean>;
    public readonly statusMenuObservavel: Observable<boolean>;

    constructor(
        private readonly http: HttpClient,
        private readonly configuracaoAplicacao: ConfiguracaoAplicacaoService
    ) {
        const dadosArmazenados = localStorage.getItem('usuarioAtual');
        const usuarioArmazenado = dadosArmazenados ? JSON.parse(dadosArmazenados) : null;
        const usuarioAtual = usuarioArmazenado ? this.mapearParaUsuario(usuarioArmazenado) : null;
        this.usuarioAtual$ = new BehaviorSubject<User | null>(usuarioAtual);
        this.usuarioAtualObservavel = this.usuarioAtual$.asObservable();
        this.statusMenu$ = new BehaviorSubject<boolean>(this.deveExibirMenu(usuarioAtual?.roles ?? []));
        this.statusMenuObservavel = this.statusMenu$.asObservable();
    }

    public obterUsuarioAtual(): User | null {
        return this.usuarioAtual$.getValue();
    }

    autenticar(login: string, senha: string) {
        const url = this.configuracaoAplicacao.construirUrlApi('/auth/login');
        const loginSanitizado = this.sanitizarTextoSimples(login);
        const senhaSanitizada = this.sanitizarTextoSimples(senha);
        return this.http.post<any>(url, { login: loginSanitizado, senha: senhaSanitizada })
            .pipe(map(resposta => {
                const usuario = this.mapearParaUsuario(resposta);
                localStorage.setItem('usuarioAtual', JSON.stringify(usuario));
                this.usuarioAtual$.next(usuario);
                this.atualizarStatusMenu(this.deveExibirMenu(usuario.roles));
                return usuario;
            }));
    }

    encerrarSessao() {
        this.atualizarStatusMenu(false);
        localStorage.removeItem('usuarioAtual');
        localStorage.removeItem('nomeUsuario');
        this.usuarioAtual$.next(null);
    }

    definirNomeUsuario(nomeUsuario: string) {
        const valorSanitizado = this.sanitizarTextoSimples(nomeUsuario);
        localStorage.setItem('nomeUsuario', JSON.stringify(valorSanitizado));
    }

    obterNomeUsuario(): string | null {
        const dadosArmazenados = localStorage.getItem('nomeUsuario');
        const valor = dadosArmazenados ? JSON.parse(dadosArmazenados) : null;
        return typeof valor === 'string' ? this.sanitizarTextoSimples(valor) : null;
    }

    atualizarStatusMenu(status: boolean) {
        this.statusMenu$.next(status);
    }

    obterStatusMenuAtual(): boolean {
        return this.statusMenu$.getValue();
    }

    possuiPapel(papel: string): boolean {
        const normalizado = papel?.startsWith('ROLE_') ? papel : `ROLE_${(papel ?? '').toUpperCase()}`;
        return this.obterPapeisAtuais().includes(normalizado);
    }

    possuiAlgumPapel(...papeis: string[]): boolean {
        if (!papeis || papeis.length === 0) {
            return false;
        }
        return papeis.some(papel => this.possuiPapel(papel));
    }

    obterPapeisAtuais(): string[] {
        return this.usuarioAtual$.getValue()?.roles ?? [];
    }

    private deveExibirMenu(papeis: string[]): boolean {
        const papeisNormalizados = this.normalizarPapeis(papeis);
        const papeisPermitidos = [
            'ROLE_ADMIN_PORTO',
            'ROLE_PLANEJADOR',
            'ROLE_OPERADOR_GATE',
            'ROLE_TRANSPORTADORA'
        ];
        return papeisNormalizados.some(papel => papeisPermitidos.includes(papel));
    }

    private mapearParaUsuario(dados: any): User {
        if (!dados) {
            return new User();
        }

        const origem = dados.data ?? dados;
        const token = origem.token
            ?? dados.token
            ?? origem.accessToken
            ?? dados.accessToken
            ?? '';
        const decodificado = this.decodificarToken(token);
        const papeisResposta = Array.isArray(origem.roles)
            ? origem.roles
            : (origem.roles ? [origem.roles] : []);
        const papeisToken = Array.isArray(decodificado?.roles)
            ? decodificado.roles
            : (decodificado?.role ? [decodificado.role] : []);
        const papeis = this.normalizarPapeis([...(papeisResposta || []), ...(papeisToken || [])]);
        const perfil = decodificado?.perfil ?? origem.perfil ?? dados.perfil ?? (papeis.length > 0 ? papeis[0] : '');
        const nome = decodificado?.nome ?? origem.nome ?? origem.name ?? origem.login ?? dados.nome ?? dados.login ?? '';
        const id = decodificado?.userId ?? origem.id ?? dados.id ?? origem.userId ?? dados.userId ?? '';
        const transportadoraDocumento = decodificado?.transportadoraDocumento ?? origem.transportadoraDocumento ?? null;
        const transportadoraNome = decodificado?.transportadoraNome ?? origem.transportadoraNome ?? null;

        return new User(
            id,
            nome,
            token,
            origem.email ?? dados.email ?? '',
            origem.senha ?? dados.senha ?? '',
            perfil,
            papeis,
            transportadoraDocumento,
            transportadoraNome
        );
    }

    private decodificarToken(token: string | undefined): any | null {
        if (!token) {
            return null;
        }
        const segmentos = token.split('.');
        if (segmentos.length < 2) {
            return null;
        }
        try {
            const cargaUtil = segmentos[1]
                .replace(/-/g, '+')
                .replace(/_/g, '/');
            const payloadDecodificado = decodeURIComponent(atob(cargaUtil)
                .split('')
                .map(caractere => '%' + ('00' + caractere.charCodeAt(0).toString(16)).slice(-2))
                .join(''));
            return JSON.parse(payloadDecodificado);
        } catch (erro) {
            console.warn('Falha ao decodificar token JWT', erro);
            return null;
        }
    }

    private normalizarPapeis(papeis: string[] | undefined): string[] {
        if (!papeis) {
            return [];
        }
        const normalizados = papeis
            .filter(papel => !!papel)
            .map(papel => papel.startsWith('ROLE_') ? papel : `ROLE_${papel.toUpperCase()}`);
        return Array.from(new Set(normalizados));
    }

    private sanitizarTextoSimples(valor: string): string {
        if (!valor) {
            return '';
        }
        return valor
            .normalize('NFKC')
            .replace(/[<>"'`\\]/g, '')
            .trim();
    }
}
