# WhatsDireto (Multiplataforma)

Inicie conversas no WhatsApp sem precisar salvar o número na agenda — agora em **Android**, **iOS**, **Desktop (Windows, macOS, Linux)** e **Web (Wasm/PWA)** com **Kotlin & Compose Multiplatform**.

## Funcionalidades

- **Validação internacional (E.164) e 40 países** com busca instantânea por nome, sigla ou DDI.
- **Colagem inteligente**: extrai automaticamente número, país e mensagem de textos comuns, links `wa.me/...`, `api.whatsapp.com/send`, `tel:` e vCards.
- **Formatação sem quebrar o cursor**: máscara via `VisualTransformation` que permite editar qualquer dígito no meio do número (e corrige DDDs iguais ao DDI, como DDD 55 no RS).
- **WhatsApp e WhatsApp Business (Android)**: seletor rápido para escolher qual app abrir, com fallback seguro para o navegador.
- **Leitor de QR Code nativo (Android)** e **Gerador de QR Code multiplataforma** em Canvas puro para qualquer link `wa.me`.
- **Mensagens rápidas editáveis**: crie, edite, exclua ou restaure modelos de resposta.
- **Histórico organizado**: busca em tempo real, fixar contatos no topo, etiquetas/apelidos locais, abertura com 1 toque, cópia de link e exclusão individual com **Desfazer**.
- **Interface adaptativa + Modo Claro e Escuro**: layout em 1 coluna no celular e 2 colunas em tablets/desktop/web, com contraste acessível (WCAG AA).

## Estrutura do Projeto

- `composeApp/src/commonMain` — Domínio, validação, QR Code, repositórios, controller e UI Compose compartilhados.
- `composeApp/src/androidMain` — Integração Android (Edge-to-Edge, `ACTION_SEND`, Contatos, CameraX + ML Kit QR Scanner, WhatsApp/Business).
- `composeApp/src/desktopMain` — Aplicativo Desktop JVM (Windows, macOS, Linux).
- `composeApp/src/wasmJsMain` — Aplicativo Web/PWA via Kotlin/Wasm.
- `composeApp/src/iosMain` + `iosApp/` — Ponte nativa iOS (`NSUserDefaults`, `UIPasteboard`, `UIApplication`) e wrapper SwiftUI para Xcode.
- `composeApp/src/commonTest` — Testes unitários multiplataforma.

## Como Executar

### Testes Unitários
```bash
./gradlew :composeApp:desktopTest
```

### Android (APK Debug)
```bash
./gradlew :composeApp:assembleDebug
```
APK gerado em `composeApp/build/outputs/apk/debug/composeApp-debug.apk`.

### Desktop (Windows / macOS / Linux)
```bash
./gradlew :composeApp:run
```
Para empacotar instalador nativo (`.msi` / `.dmg` / `.deb`):
```bash
./gradlew :composeApp:packageDistributionForCurrentOS
```

### Web (Browser / PWA)
```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```
Para gerar o bundle de produção:
```bash
./gradlew :composeApp:wasmJsBrowserDistribution
```

### Deploy Automático na Vercel (via GitHub)
Este repositório já inclui o workflow [`.github/workflows/deploy-web.yml`](.github/workflows/deploy-web.yml) e [`vercel.json`](vercel.json):

1. Faça push na branch `main` — o **GitHub Actions** executará os testes, compilará o bundle Web (Kotlin/Wasm) e publicará os arquivos prontos automaticamente na branch `web-dist`.
2. Na **Vercel**, clique em **Add New → Project** e importe o repositório `WhatsDireto`.
3. Em **Settings → Git → Production Branch**, altere para **`web-dist`** (Framework Preset: `Other`, Build Command vazio, Output Directory vazio/`.`).
4. Pronto! Todo push na `main` gera uma nova versão e a Vercel publica automaticamente em segundos.

### iOS (macOS + Xcode)
1. Abra `iosApp/` em um projeto Xcode apontando para o framework `:composeApp:embedAndSignAppleFrameworkForXcode`.
2. Execute no simulador ou dispositivo físico iOS.
