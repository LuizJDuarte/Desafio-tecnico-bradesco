# Desafio-tecnico-bradesco
Desafio técnico do Bradesco de um software replicador de Banco de Dados, responsável por copiar informações de um Banco de Origem (sistema operacional de um posto de combustível) para um ou mais Bancos de Destino, com base em configurações armazenadas em um Banco de Controle (Gerencial).

## Como funciona

A cada ciclo (60 segundos), para cada direção habilitada (uma origem e um destino):

1. **Carga completa:** lê as tabelas configuradas, na ordem do campo `ORDEM` e com o filtro `DS_WHERE` se houver, e insere no destino as linhas que ainda não existem lá.
2. **Incremental:** aplica no destino as alterações (insert, update e delete) que os triggers da origem registraram na `replication_queue`. O ponto até onde cada direção já aplicou fica na `TB_REPLICACAO_CHECKPOINT`, no Banco de Controle, então vários destinos da mesma origem recebem todas as alterações.

Uma origem ou destino fora do ar não impede as outras direções. A direção que falhou continua de onde parou no ciclo seguinte.

O `DS_WHERE` filtra só a carga completa; as alterações da fila são aplicadas para qualquer linha das tabelas configuradas.

A `replication_queue` cresce com o tempo. Eventos com `id` menor que o menor `ULTIMO_ID_FILA` das direções daquela origem já chegaram a todos os destinos e podem ser apagados.

## Banco de dados

Os scripts ficam em `sql/` e podem ser executados mais de uma vez:

| Script | Onde rodar |
|---|---|
| `controle.sql` | Banco de Controle. Cria as tabelas e um processo de exemplo com as 4 tabelas do posto |
| `origem.sql` | Banco Origem. Cria `funcionarios`, `clientes`, `bombas`, `abastecimentos`, a `replication_queue` e os triggers |
| `origem_dados_exemplo.sql` | Banco Origem, opcional. Dados para teste |
| `destino.sql` | Cada Banco Destino |

```powershell
psql -U postgres -c "CREATE DATABASE controle"
psql -U postgres -c "CREATE DATABASE posto"
psql -U postgres -c "CREATE DATABASE posto_destino1"

psql -U postgres -d controle -f sql/controle.sql
psql -U postgres -d posto -f sql/origem.sql
psql -U postgres -d posto -f sql/origem_dados_exemplo.sql
psql -U postgres -d posto_destino1 -f sql/destino.sql
```

No Windows, o `psql` fica em `C:\Program Files\PostgreSQL\<versão>\bin`. Também dá para abrir cada arquivo no Query Tool do pgAdmin.

Depois, abra a aplicação, vá em **Cadastro > Direções** e cadastre a origem e o destino, por exemplo `jdbc:postgresql://localhost:5432/posto` e `jdbc:postgresql://localhost:5432/posto_destino1`. As direções são cadastradas pela tela para que as senhas sejam gravadas criptografadas.

Toda tabela replicada precisa de chave primária `id` do tipo BIGINT, e os destinos precisam ser PostgreSQL.

## Configuração

Dados sensíveis vêm de variáveis de ambiente, nunca do código ou do repositório.

| Variável | Descrição | Exemplo |
|---|---|---|
| `REPLICADOR_CONTROLE_URL` | URL JDBC do Banco de Controle | `jdbc:postgresql://localhost:5432/controle` |
| `REPLICADOR_CONTROLE_USUARIO` | Usuário do Banco de Controle | `postgres` |
| `REPLICADOR_CONTROLE_SENHA` | Senha desse usuário | |
| `REPLICADOR_CHAVE` | Chave de 32 bytes em Base64 que criptografa as senhas de origem e destino | |

Para desenvolvimento, copie o `.env.example` para `.env` e preencha. O `.env` está no `.gitignore` e não deve ser commitado.

Para gerar a chave:

```powershell
# Windows (PowerShell)
$b = New-Object byte[] 32; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)
```

```bash
# Linux
openssl rand -base64 32
```

Guarde a chave junto com as outras credenciais. Sem ela não dá para ler as senhas já cadastradas. Se ela for perdida ou trocada, abra cada direção na tela e digite as senhas de novo.

## Execução

O `Main` recebe um argumento que define o modo:

| Argumento | Modo |
|---|---|
| `servico=yes` | Executa a replicação a cada 60 segundos, sem interface (uso como serviço) |
| `servico=no` | Abre a interface: cadastros, **Sistema > Executar Replicação** e **Sistema > Parar Replicação** |

### IntelliJ IDEA

1. Abra **Run > Edit Configurations...** e selecione `Main`.
2. Em **Program arguments**, informe `servico=no` ou `servico=yes`.
3. Em **Environment variables**, informe o caminho do arquivo `.env`.

### Windows (PowerShell)

```powershell
$env:REPLICADOR_CONTROLE_URL = "jdbc:postgresql://localhost:5432/controle"
$env:REPLICADOR_CONTROLE_USUARIO = "postgres"
$env:REPLICADOR_CONTROLE_SENHA = "sua-senha"
$env:REPLICADOR_CHAVE = "sua-chave-em-base64"

javac -d out/classes -cp postgresql-42.7.13.jar (Get-ChildItem -Recurse src -Filter *.java).FullName
java -cp "out/classes;postgresql-42.7.13.jar" Main servico=no
```

Variáveis definidas com `$env:` valem só para a janela atual do PowerShell.

### Linux (serviço)

```bash
set -a; . ./.env; set +a
javac -d out/classes -cp postgresql-42.7.13.jar $(find src -name '*.java')
java -cp "out/classes:postgresql-42.7.13.jar" Main servico=yes
```

Para rodar com o systemd, guarde as variáveis num arquivo que só o usuário do serviço consegue ler (`chmod 600 /etc/replicador/replicador.env`) e aponte para ele:

```ini
[Unit]
Description=Replicador de banco de dados
After=network-online.target

[Service]
User=replicador
WorkingDirectory=/opt/replicador
EnvironmentFile=/etc/replicador/replicador.env
ExecStart=/usr/bin/java -cp out/classes:postgresql-42.7.13.jar Main servico=yes
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

## Segurança

- Credenciais do Banco de Controle e a chave de criptografia ficam só em variáveis de ambiente.
- As senhas de origem e destino são gravadas com AES-256-GCM. A tela não mostra a senha gravada: na edição, campo em branco mantém a atual.
- Nomes de tabela são validados (letras, números e `_`), nomes de coluna vão entre aspas e o `DS_WHERE` não aceita `;` nem comentários. A carga completa lê a origem em transação somente leitura.
- URLs de conexão precisam começar com `jdbc:postgresql:` e não podem conter a senha. Nos logs e na consulta de direções a URL aparece mascarada.

Recomendações para o ambiente:

- Na origem, use um usuário só com `SELECT` nas tabelas replicadas e na `replication_queue`. O replicador não escreve na origem.
- No destino, use um usuário com `SELECT`, `INSERT`, `UPDATE` e `DELETE` só nas tabelas replicadas.
- Restrinja quem pode alterar o Banco de Controle: quem altera uma direção consegue apontar a replicação para outro servidor.
- Em conexões fora da máquina local, use `?sslmode=require` na URL.
