@echo off
setlocal EnableExtensions
title Deploy WhatsDireto para GitHub

echo =====================================
echo      Deploy do Projeto WhatsDireto
echo =====================================
echo.

set /p APP_VERSION=Digite a versao do app (ex: 1.0.0): 
if "%APP_VERSION%"=="" (
    echo Versao nao informada. Encerrando.
    pause
    exit /b 1
)

set "REPO_NAME=WhatsDireto"
set "BRANCH=main"
set "REMOTE_URL="

echo.
echo Versao informada: %APP_VERSION%
echo.

where git >nul 2>&1
if errorlevel 1 (
    echo Git nao encontrado.
    pause
    exit /b 1
)

where gh >nul 2>&1
if errorlevel 1 (
    echo GitHub CLI nao encontrado.
    echo Instale com: winget install GitHub.cli
    pause
    exit /b 1
)

echo Verificando autenticacao no GitHub...
gh auth status >nul 2>&1
if errorlevel 1 (
    echo Voce nao esta autenticado.
    echo Rode primeiro: gh auth login
    pause
    exit /b 1
)

echo.
if not exist ".git" (
    echo Inicializando repositorio Git...
    git init
)

if not exist ".gitignore" (
    echo Criando .gitignore Android...
    (
        echo # Android
        echo .gradle/
        echo /build
        echo /local.properties
        echo *.iml
        echo .idea/
        echo *.apk
        echo *.ap_
        echo *.dex
        echo *.class
        echo captures/
        echo .externalNativeBuild/
        echo .cxx/
    ) > .gitignore
)

echo.
echo Ajustando branch...
git branch -M %BRANCH% >nul 2>&1

echo.
echo Adicionando arquivos...
git add .

echo.
echo Criando commit...
git diff --cached --quiet
if errorlevel 1 (
    git commit -m "release v%APP_VERSION%"
) else (
    echo Nenhuma alteracao nova para commit.
)

echo.
echo Verificando remote origin...
git remote get-url origin >nul 2>&1
if not errorlevel 1 goto PUSH_CODE

echo Remote origin nao existe. Verificando repo online...
gh repo view "%REPO_NAME%" >nul 2>&1
if not errorlevel 1 goto ADD_REMOTE_EXISTING

echo Repositorio online nao existe. Tentando criar...
gh repo create "%REPO_NAME%" --public --source=. --remote=origin
if errorlevel 1 (
    echo.
    echo Falha ao criar repositorio no GitHub.
    echo Causa provavel: autenticacao sem permissao para createRepository.
    echo.
    echo Rode estes comandos e tente novamente:
    echo   gh auth logout
    echo   gh auth login
    echo.
    echo Dica: escolha login pelo navegador.
    pause
    exit /b 1
)
goto PUSH_CODE

:ADD_REMOTE_EXISTING
echo Repositorio online ja existe. Obtendo URL...
for /f "delims=" %%R in ('gh repo view "%REPO_NAME%" --json url -q ".url"') do set "REMOTE_URL=%%R"

if "%REMOTE_URL%"=="" (
    echo Nao foi possivel obter a URL do repositorio.
    pause
    exit /b 1
)

git remote add origin "%REMOTE_URL%"

:PUSH_CODE
echo.
echo Enviando para GitHub...
git push -u origin %BRANCH%
if errorlevel 1 (
    echo Falha no push.
    pause
    exit /b 1
)

echo.
echo =====================================
echo Deploy concluido com sucesso!
echo =====================================
echo Commit: release v%APP_VERSION%
echo Branch: %BRANCH%

for /f "delims=" %%R in ('gh repo view "%REPO_NAME%" --json url -q ".url"') do echo Repositorio: %%R

echo.
pause