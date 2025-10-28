@echo off
setlocal enabledelayedexpansion

:: Obter o nome da pasta atual
for %%I in (.) do set "CURRENT_FOLDER=%%~nxI"

:: Obter a data e hora no formato yyyymmddhhmmss
for /f "tokens=2 delims==" %%I in ('wmic os get localdatetime /value') do set datetime=%%I
set OUTPUT_FILE=%CURRENT_FOLDER%_%datetime:~0,4%%datetime:~4,2%%datetime:~6,2%%datetime:~8,2%%datetime:~10,2%%datetime:~12,2%.txt

:: Criar o arquivo de saída
echo Gerando %OUTPUT_FILE%...

:: Percorrer todos os arquivos .java, .xml e .xhtml nas pastas e subpastas
for /r %%F in (*.css *.env *.gitignore *.html *.java *.js *.json *.jsx *.md *.properties *.scss *.sample *.sql *.ts *.tsx *.vue *.webmanifest *.xhtml *.xml *.gradle) do (
    echo ----- %%F ----- >> "%OUTPUT_FILE%"
    type "%%F" >> "%OUTPUT_FILE%"
    echo. >> "%OUTPUT_FILE%"
)

echo Processo concluído. Arquivo gerado: %OUTPUT_FILE%