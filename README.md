# 🍅 Pomodoro Focus Timer

<p align="center">
<img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android Badge"/>
<img src="https://img.shields.io/badge/Kotlin-0095D5?&style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin Badge"/>
<img src="https://img.shields.io/badge/Material_Design-757575?style=for-the-badge&logo=material-design&logoColor=white" alt="Material Design Badge"/>
</p>

Um aplicativo nativo para Android desenvolvido com o intuito de aplicar os conceitos de **Foreground Services (Serviços de Primeiro Plano)** para gerenciar contagens regressivas de foco (técnica Pomodoro) de forma segura em segundo plano.
O foco principal do desenvolvimento deste projeto foi lidar com as exigências modernas do sistema operacional Android (APIs 24 a 34), que impõem restrições rigorosas à execução de serviços contínuos e exibição de notificações.
## ✨ Funcionalidades
**Ciclo de Vida Misto:** Implementação conjunta dos modos de serviço *Started* (independência da Activity) e *Bound* (vinculação reativa à tela).
**Controles em Segundo Plano:** Play, Pause e Stop não param quando a tela é bloqueada ou minimizada.
**Notificações Interativas:** Botões injetados dinamicamente na notificação via `PendingIntent`.
**UI Responsiva e Fluida:** Construída através de layouts XML padronizados (FrameLayout e MaterialCardView), usando Material Design 3.
**Atualização em Tempo Real:** Sem travamento do aplicativo ou recarga da Activity, comunicando o Serviço com a Activity através de fluxos de dados modernos.
## 🛠️ Tecnologias e Arquitetura

O aplicativo foi arquitetado utilizando bibliotecas limpas e recursos nativos orientados ao ecossistema Jetpack moderno.

**Kotlin:** Linguagem base de todo o projeto.
**Coroutines:** Gerenciamento da repetição cronometrada fora da *Main Thread* (via `Dispatchers.Default`), evitando o erro de ANR (Application Not Responding).
**StateFlow:** Canal reativo para a transmissão assíncrona dos estados do temporizador (Running, Paused, Stopped) e dos segundos atualizados, sendo coletados de forma "segura" (`repeatOnLifecycle`) pela Activity.
**View Binding:** Substituição segura e *null-safe* do `findViewById`.
**Android Notifications:** Tratamento dinâmico de instâncias utilizando `NotificationCompat` e permissões de tempo de execução (`POST_NOTIFICATIONS`) obrigatórias em Android 13+.
**Foreground Service Tipo especial (Android 14+):** Configuração `specialUse` para respeitar as novas exigências estritas da Google Play.

## 🚀 Como testar localmente

Pré-requisitos:
**Android Studio** atualizado (versão Jellyfish/Koala ou superior).
Emulador ou aparelho físico (API 24 / Android 7.0 no mínimo).
1. Clone o repositório
   git clone git@github.com:seu-usuario/Pomodoro-Focus-Timer.git
2. Acesse a pasta do projeto
   cd Pomodoro-Focus-Timer

3. Compile com o Gradle Wrapper embutido (independente de instalação do Gradle local)
   ./gradlew assembleDebug

Após o build, abra a pasta do projeto dentro do seu Android Studio e clique no botão verde "Run" ou digite no terminal para instalar diretamente em seu emulador logado:
./gradlew installDebug


## 🛡️ Gestão de Permissões Críticas

O aplicativo lida proativamente com a evolução rígida do ecossistema Android configurando no `AndroidManifest.xml`:
*   `FOREGROUND_SERVICE`
*   `FOREGROUND_SERVICE_SPECIAL_USE` (Exigido Android 14)
*   `POST_NOTIFICATIONS` (Pedido através de launcher em *Runtime* para Android 13+)
---

>Desenvolvido para estudos em aprofundamento do núcleo do Android.