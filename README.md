# azure-services-lab-springboot

Spring Boot 에서 각종 Azure 서비스 연동을 구현하는 레퍼런스 프로젝트입니다.

## Project Objectives

- Spring Boot Starter, 공식 Azure SDK 에 맞춘 주요 설정
- Microsoft Entra ID 기반 인증 및 접근 제어
- 각 Azure 서비스별 기본 사용 예제

## Supported Azure Services

- Azure Managed Redis

  | Key | Value |
  | --- | --- |
  | 패키지명 | `azure.services.lab.managedredis` |
  | Managed Identity 기반 설정 | `application-managed-redis-with-managed-identity.yaml` |
  | Service Principal 기반 설정 | `application-managed-redis-with-service-principal.yaml` |
