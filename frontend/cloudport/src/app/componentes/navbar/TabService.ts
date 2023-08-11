import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';


@Injectable({
  providedIn: 'root'
})
export class TabService {
  private tabsSubject = new BehaviorSubject<string[]>([]);
  tabs$ = this.tabsSubject.asObservable();
  private contentSubject = new BehaviorSubject<any>(null);
  content$ = this.contentSubject.asObservable();

  // Armazenamento para o conteúdo de cada aba
  private tabContents: { [tabName: string]: any } = {};


  
  

  /*
  openTab(tab: string, content?: any) {
    console.log(`Classe TabService: Método openTab chamado com o parâmetro tab=${tab}.`);
    console.log(`Classe TabService: Método openTab chamado com o parâmetro content=${content}.`);
    const tabs = this.tabsSubject.value;
    if (!tabs.includes(tab)) {
      this.tabsSubject.next([...tabs, tab]);
      if (content) {
        this.tabContents[tab] = content;
        console.log(`Classe TabService: Conteúdo definido para a aba ${tab}:`, content);
      } else {
        console.log(`Classe TabService: Nenhum conteúdo fornecido para a aba ${tab}. A aba foi aberta sem conteúdo.`);
      }
    } else {
      console.log(`Classe TabService: A aba ${tab} já está aberta.`);
    }
  }

  */
  openTab(tab: string, content?: any) {
    const tabs = this.tabsSubject.value;
    if (!tabs.includes(tab)) {
      this.tabsSubject.next([...tabs, tab]);
      if (content) {
        this.tabContents[tab] = content;
      }
    }
  }

  

  /*
  
    // Método para abrir uma nova aba
    openTab(tab: string) {
      const tabs = this.tabsSubject.value;
      if (!tabs.includes(tab)) {
        this.tabsSubject.next([...tabs, tab]);
      }
    }

    */
/*
    openTab(tab: string) {
      const tabs = this.tabsSubject.value;
      if (!tabs.includes(tab)) {
        // Criar o conteúdo para a aba
        const content = { message: `Conteúdo padrão para a aba ${tab}` };
    
        // Armazenar o conteúdo no objeto tabContents
        this.tabContents[tab] = content;
    
        // Adicionar a aba à lista de abas
        this.tabsSubject.next([...tabs, tab]);
      }
    }
    */
    setContent(content: any) {
      this.contentSubject.next(content);
    }
    
    
  closeTab(tab: string) {
    console.log(`Classe TabService: Método closeTab chamado com o parâmetro tab=${tab}.`);
    const tabs = this.tabsSubject.value;
    this.tabsSubject.next(tabs.filter(t => t !== tab));
    // Remova o conteúdo da aba quando ela for fechada
    delete this.tabContents[tab];
    console.log(`Classe TabService: Conteúdo removido para a aba ${tab}.`);
  }

  getTabContent(tab: string): any {
    console.log(`Classe TabService: Método getTabContent chamado para buscar o conteúdo da aba ${tab}.`);
    const content = this.tabContents[tab];
    if (content) {
      console.log(`Classe TabService: Conteúdo obtido para a aba ${tab}:`, content);
    } else {
      console.log(`Classe TabService: Nenhum conteúdo encontrado para a aba ${tab}.`);
    }
    return content;
  }

  setTabContent(tab: string, content: any) {
    console.log(`Classe TabService: Método setTabContent chamado para definir o conteúdo da aba ${tab}.`);
    this.tabContents[tab] = content;
    console.log(`Classe TabService: Conteúdo definido para a aba ${tab}:`, content);
  }
}
