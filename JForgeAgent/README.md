# 💻 JForgeAgent - Orquestrador Autônomo Tri-Agente

O **JForgeAgent** não é um mero script; é uma Entidade de Engenharia de Software Autônoma (AGI local) estruturada sobre o Google ADK e o **JBang**. Toda tarefa que você jogar para ele será resolvida de três maneiras: Construindo e rodando Código Nativo do zero, Pesquisando na Web profunda com RAG, ou Dialogando formalmente com o contexto.

---

## 🚀 Como Iniciar

Certifique-se de que o JBang e as suas chaves de API (`GEMINI_API_KEY`) no ambiente estão prontas e dispare no seu projeto:
```bash
jbang .\JForgeAgent.java
```

---

## 🧠 Arquitetura: Como a Máquina Pensa (V6.2)

O JForge roda num "Loop de Orquestração" assustadoramente resiliente controlado por **Três Agentes** distintos:

1. **O Diretor (Router Agent):** Você fala com ele primeiro. Ele lê as suas intenções (Intents) cruas, checa o relógio do sistema, lê o cache do disco rígido e decide se deve gerar código (`CREATE`), executar código velho (`EXECUTE`) ou raspar a web (`SEARCH`).
2. **O Forjador (Coder Agent):** O operário cego e mudo. Se o Router mandar um `CREATE` ou `EDIT`, este agente acorda no escuro, baixa dependências, escreve arquivos nativos robustos em Java (incluindo JFiglet, Swing, HTTP Clients) no seu disco rígido e submete para teste.
3. **O Representante (Assist Agent):** Cuida da formatação visual e comunicação humana. Diferente de sistemas rasos, o nosso Assistente não tem "alucinações". Se o seu prompt requereu dados (ex: Preço do Dólar), a Orquestração já pesquisou a internet (RAG) pelo DuckDuckGo e entregou o texto quente mastigado para o Assistente ler. O Assistente também sabe instantaneamente quais ferramentas estão instaladas na pasta `/tools` do disco.

> **💀 Auto-Heal (Auto-Cura Automática):** Se uma ferramenta criada der "Crash" por causa de uma permissão do Windows negada ou um erro de Thread do Java, o loop recolhe o `StackTrace` vermelho em tela e manda de volta pro *Coder*. Ele recompila o erro na unha na sua frente enquanto ele próprio se conserta!

> **🗑️ Cognitive Garbage Collection (Esquecimento):** Baseado em um Cache L.R.U. (Least Recently Used). O JForge apaga do seu HD as ferramentas mais poeirentas e esquecidas na gaveta assim que as habilidades do motor passarem de 10 utilitários em Cache. Ele sabe cuidar do próprio lixo.

---

## 🌟 A Forma Mais Eficiente de Pilotart o JForge

A partir da nossa minuciosa análise de logs da versão V6, aqui estão os segredos para arrancar o máximo desse colosso autônomo sem tropeços:

### 1. Diálogos Factuais Rápidos (Sem criação de Ferramentas)
Perguntas curtas factuais são absorvidas pelo escudo conversacional (*Assistente + Web Search*). 
- **Faça:** *"Qual a cotação atual do Bitcoin e as tendências de mercado?"* ou *"Quem ganhou a copa?"*
- **Acontece:** Ele varre a Web de forma invisível desviando passivamente dos Captchas, injeta a data local para não errar dia/hora e te devolve conhecimento puro em texto.

### 2. A Invocação de Código "Forjado"
Para forçar o motor a criar uma Ferramenta local permanente no seu disco, use gatilhos processuais ou mecânicos imperativos.
- **Faça:** *"Crie uma rotina/ferramenta/app para..."* ou ordens diretas de ação: *"Delete o arquivo..."*
- **Acontece:** O Diretor engata o `CREATE`. Ele puxará bibliotecas da internet (usando tags `//DEPS` do JBang), forjará o arquivo `.java` respectivo em `/tools`, conectará na API solicitada e rodará na sua máquina nativa sem pedir licença! 

**💡 Galeria de Prompts Geradores (Para forçar Automação Avançada):**
- **🧮 Cálculos Matemáticos e Algoritmos:** *"Escreva uma ferramenta usando a biblioteca Apfloat para calcular Pi até a 500ª casa decimal."*
- **📡 Consumo de APIs Autenticadas:** *"Forje um script que bata na API do GitHub usando esse token (XXXX) e me devolva os pull requests abertos."*
- **🖥️ Interfaces Gráficas (GUIs):** *"Construa um aplicativo gráfico (Swing ou JavaFX) que plote um gráfico de pizza mostrando a cotação do Ouro usando a biblioteca XChart."*
- **📁 Engenharia de File System Local:** *"Faça um crawler que leia minha pasta de Downloads, encontre todos os arquivos `.pdf` maiores que 5MB e mova eles silenciosamente para a pasta Meus Documentos."*

### 3. Abreviei, Não Expliquei (O Poder do Cache)
Se uma ferramenta já foi construída há algumas mensagens atrás, o Orquestrador injetou as metas-schemas `.json` na sua Córtex. Responda como se falasse com um humano.
- **No passado você pediu:** *"Qual o clima no RJ via script java wttr.in?"*
- **Faça AGORA:** *"E o de Nova York?"*
- **Acontece:** Ele interpretará o verbo perdido, detectará as ferramentas cacheadas e bradará um `EXECUTE: WeatherFetcher.java "Nova York"`. Em 1 milissegundo a ferramenta será rodada sem ir para a Web.

### 4. Forçando Fallbacks Extremamente Avançados 
O que acontece se a Web Search falhar e o Assistente esbarrar numa proteção anti-bot da vida real? 
Se o Roteador estiver lidando com uma demanda essencial que o bloqueou, ele muda de estratégia cirurgicamente: ele abandonará o modo Assistente de Chat e *forjará intencionalmente e localmente* um cliente Python / Java customizado que bate numa API livre para burlar os bloqueios. Não há muros inquebráveis neste sistema.
