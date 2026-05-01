# OTP Security Service

Сервис для генерации и проверки одноразовых кодов (OTP) с отправкой через Email, SMS и Telegram.

## Как запустить сервис

1. Установите PostgreSQL 14
2. Создайте базу данных: `otp_security`
3. Выполните SQL скрипт (таблицы `users`, `otp_config`, `otp_codes`)
4. Откройте проект в IDEA
5. Запустите `MainServer.java`
6. Сервер запустится на `http://localhost:8080`

## API Эндпоинты

| Метод | URL | Описание | Требуется токен |
|-------|-----|----------|-----------------|
| POST | `/register` | Регистрация пользователя | Нет |
| POST | `/login` | Логин, получение JWT токена | Нет |
| POST | `/user/generate` | Сгенерировать OTP код | Да |
| POST | `/user/validate` | Проверить OTP код | Да |
| PUT | `/admin/config` | Изменить конфигурацию OTP | Да (ADMIN) |
| GET | `/admin/users` | Список пользователей | Да (ADMIN) |
| DELETE | `/admin/users/{id}` | Удалить пользователя | Да (ADMIN) |

## Как тестировать (Postman)

### 1. Регистрация
POST http://localhost:8080/register
Body: {"login":"user1","password":"123456"}

### 2. Логин (получить токен)
POST http://localhost:8080/login
Body: {"login":"user1","password":"123456"}

### 3. Генерация кода (с токеном)
POST http://localhost:8080/user/generate
Headers: Authorization: <токен>
Body: {"operationId":"op1","contact":"email@example.com"}

### 4. Проверка кода
POST http://localhost:8080/user/validate
Headers: Authorization: <токен>
Body: {"operationId":"op1","code":"123456"}

## Технологии

- Java 17
- PostgreSQL 14
- Maven
- JWT аутентификация
- Telegram Bot API
- Jakarta Mail (Email)
- JSMPP (SMS)
