# Renan AI — Criador de Vídeos com IA

Aplicação Spring Boot + Thymeleaf para geração de vídeos com **Amazon Nova Reel** (AWS Bedrock).

## ✨ Funcionalidades

- Geração de vídeos 1280×720 via Amazon Nova Reel
- Suporte a imagem de referência (redimensionamento automático para 1280×720)
- Prompt máximo de 512 caracteres com contador em tempo real
- Download com remoção automática do S3 após consumo
- TTL de 60 minutos: vídeos abandonados são deletados automaticamente
- Interface responsiva com fila de geração assíncrona

## ⚙️ Pré-requisitos

- Java 21+
- Maven 3.8+
- Conta AWS com acesso ao **Amazon Bedrock** (modelo `amazon.nova-reel-v1:0`)
- Bucket S3 com política para `bedrock.amazonaws.com` ser principal

## 🚀 Como rodar

### 1. Credenciais AWS

Copie o template e preencha com suas chaves:

```bash
cp src/main/resources/application-local.properties.template \
   src/main/resources/application-local.properties
```

Edite `application-local.properties`:
```properties
aws.access-key-id=SUA_ACCESS_KEY
aws.secret-access-key=SUA_SECRET_KEY
```

> `application-local.properties` está no `.gitignore` e **nunca será commitado**.

### 2. Configurar o bucket S3

Em `application.properties`, ajuste:
```properties
aws.s3.output-bucket=nome-do-seu-bucket
```

O bucket precisa de uma política permitindo que o Bedrock escreva nele:
```json
{
  "Effect": "Allow",
  "Principal": { "Service": "bedrock.amazonaws.com" },
  "Action": "s3:PutObject",
  "Resource": "arn:aws:s3:::nome-do-seu-bucket/*",
  "Condition": {
    "StringEquals": { "aws:SourceAccount": "SEU_ACCOUNT_ID" }
  }
}
```

### 3. Iniciar a aplicação

```bash
mvn spring-boot:run
```

Acesse: http://localhost:8080/video

## 🔐 Segurança

As credenciais AWS **nunca ficam no repositório**. O fluxo é:

| Arquivo | Git | Descrição |
|---|---|---|
| `application.properties` | ✅ commitado | Configurações sem segredos |
| `application-local.properties` | ❌ ignorado | Suas chaves reais (local only) |
| `application-local.properties.template` | ✅ commitado | Modelo para novos devs |

Alternativamente, use variáveis de ambiente:
```bash
export AWS_ACCESS_KEY_ID=sua_key
export AWS_SECRET_ACCESS_KEY=sua_secret
mvn spring-boot:run
```

## 🛠️ Stack

- Java 21 / Spring Boot 3.2.3
- Thymeleaf
- AWS SDK v2 2.42.9 (Bedrock Runtime + S3)
- Bootstrap Icons
