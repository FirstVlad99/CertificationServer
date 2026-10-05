--liquibase formatted sql

--changeset FirstVlad99:001-01-init-schema
CREATE TABLE roles (
   id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
   name VARCHAR(20) NOT NULL UNIQUE
);
INSERT INTO roles (name) VALUES ('CUSTOMER'), ('MEMBER'),('ADMIN');

CREATE TABLE user_system_statuses (
  id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  name VARCHAR(20) NOT NULL UNIQUE
);
INSERT INTO user_system_statuses (name) VALUES ('ACTIVE'), ('INACTIVE'), ('BANNED');

CREATE TABLE person_types(
                             id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                             name VARCHAR(20) NOT NULL UNIQUE
);
INSERT INTO person_types (name) VALUES ('INDIVIDUAL'),('LEGAL'),('SOLE'); -- физ. лицо, юр. лицо, ИП

CREATE TABLE users(
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- ник, пример : @id000002
    username VARCHAR(20) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(50),
    surname VARCHAR(50),
    password_hash VARCHAR(255) NOT NULL,
    person_type_id INT REFERENCES person_types(id),
    bio VARCHAR(200),
    photo_id INT,
    site_url VARCHAR(500),
    site_label VARCHAR(20),
    system_status_id INT NOT NULL DEFAULT 1 REFERENCES user_system_statuses(id),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- repository_files - метаданные объекта в S3-бакете. Владельца (кто использует файл: сообщение, аватарка) в самой
-- таблице нет: это сообщение/аватарка (users.photo_id) ссылается, а не наоборот.
-- content_type - MIME реального объекта (open-ended), справочник FILE/LINK/TEXT тут не при чём.
-- upload_status: presigned-загрузка создаёт строку ДО реального PUT в S3 - PENDING-строки скрыты из
-- выдачи, пока confirm (с server-side HeadObject) не переведёт их в CONFIRMED, иначе в списке
-- материалов висел бы битый файл. Прямая загрузка создаёт строку сразу CONFIRMED.
-- size_bytes nullable: у presigned размер неизвестен до confirm (берём из HeadObject
-- Content-Length); прямая загрузка проставляет его сразу (bytes.length).
CREATE TABLE repository_files (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    s3_key VARCHAR(1024) NOT NULL UNIQUE,
    name TEXT NOT NULL,
    size_bytes BIGINT CHECK (size_bytes >= 0),
    mime_type VARCHAR(255) NOT NULL,
    uploaded_status VARCHAR(20) CHECK (uploaded_status IN ('PENDING', 'CONFIRMED')),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    uploaded_by INT NOT NULL REFERENCES users (id) ON DELETE RESTRICT
);

ALTER TABLE users ADD CONSTRAINT fk_users_photo FOREIGN KEY (photo_id) REFERENCES repository_files (id);

CREATE TABLE user_roles (
    user_id    INT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    role_id    INT NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    granted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE user_bans (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id INT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    reason VARCHAR(50) NOT NULL,
    banned_by INT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    banned_at TIMESTAMPTZ DEFAULT NOW(),
    expires_at TIMESTAMPTZ,
    lifted_at TIMESTAMPTZ,
    lifted_by INT REFERENCES users(id) ON DELETE RESTRICT
);

CREATE UNIQUE INDEX uq_user_bans_active ON user_bans(user_id) WHERE lifted_at IS NULL;

CREATE TABLE phone_numbers(
    user_id INT REFERENCES users(id) ON DELETE RESTRICT,
    phone_number TEXT NOT NULL UNIQUE,
    location TEXT NOT NULL CHECK (location IN ('MOBILE','HOME','WORK')),
    PRIMARY KEY (user_id, phone_number)
);

CREATE TABLE chat_roles (
                            id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                            name VARCHAR(20) NOT NULL UNIQUE
);
INSERT INTO chat_roles (name) VALUES ('MEMBER'), ('OWNER');

-- last_message_id без ON DELETE CASCADE т.к. нужно отображать
-- последнее сообщение чата, если они есть
-- гарант - транзакция удаления сообщения в Spring Boot
-- (если оно последнее в чате - нужно last_message_id навесить предыдущему)
CREATE TABLE chats (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    is_group BOOLEAN DEFAULT FALSE,
    last_message_id BIGINT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE messages_types (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(20) NOT NULL UNIQUE
);
INSERT INTO messages_types(name) VALUES ('TEXT'),('FILE');

CREATE TABLE messages (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sender_id INT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    type_id INT NOT NULL REFERENCES messages_types(id),
    chat_id BIGINT NOT NULL REFERENCES chats(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE chats ADD CONSTRAINT fk_chats_last_message FOREIGN KEY (last_message_id) REFERENCES messages (id);

CREATE TABLE message_texts (
   message_id BIGINT PRIMARY KEY REFERENCES messages(id) ON DELETE CASCADE,
   content VARCHAR(1000) NOT NULL
);

CREATE TABLE message_files (
   message_id BIGINT PRIMARY KEY REFERENCES messages(id) ON DELETE CASCADE,
   file_id BIGINT REFERENCES repository_files(id) ON DELETE CASCADE
);

CREATE INDEX idx_message_files_file_id ON message_files(file_id);

CREATE TABLE chat_participants(
    chat_id BIGINT REFERENCES chats(id) ON DELETE CASCADE,
    user_id INT REFERENCES users(id) ON DELETE RESTRICT,
    joined_at TIMESTAMPTZ DEFAULT NOW(),
    role_id INT NOT NULL REFERENCES chat_roles(id),
    PRIMARY KEY (chat_id, user_id)
);

--changeset FirstVlad99:001-02-organization
--comment: added tables related to organizations
CREATE TABLE organization_system_statuses (
    id   INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(20) NOT NULL UNIQUE
);

INSERT INTO organization_system_statuses (name)
VALUES ('ACTIVE'), ('INACTIVE'), ('BANNED');

CREATE TABLE organizations_types (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(20) NOT NULL UNIQUE
);
INSERT INTO organizations_types (name) VALUES ('CENTER'), ('BODY'),('LAB');

CREATE TABLE countries (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    --из ISO 3166-1 alpha-2 коды стран (RU,BY,KZ)
    code VARCHAR(2) NOT NULL UNIQUE
);

INSERT INTO countries(name,code) VALUES
    ('Российская Федерация','RU'),
    ('Республика Беларусь','BY'),
    ('Республика Казахстан','KZ');

CREATE TABLE regions (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    country_id INT REFERENCES countries(id) ON DELETE RESTRICT,
    name VARCHAR(100) NOT NULL,
    -- код из ОКАТО (например, Республика Карелия - 10)
    code VARCHAR(2) NOT NULL,
    UNIQUE (country_id,name),
    UNIQUE (country_id,code)
);

INSERT INTO regions(country_id,name,code) VALUES
    (1, 'Республика Адыгея', '01'),
    (1, 'Республика Алтай', '04'),
    (1, 'Республика Башкортостан', '02'),
    (1, 'Республика Бурятия', '03'),
    (1, 'Республика Дагестан', '05'),
    (1, 'Республика Ингушетия', '06'),
    (1, 'Кабардино-Балкарская Республика', '07'),
    (1, 'Республика Калмыкия', '08'),
    (1, 'Карачаево-Черкесская Республика', '09'),
    (1, 'Республика Карелия', '10'),
    (1, 'Республика Коми', '11'),
    (1, 'Республика Крым', '91'),
    (1, 'Республика Марий Эл', '12'),
    (1, 'Республика Мордовия', '13'),
    (1, 'Республика Саха (Якутия)', '14'),
    (1, 'Республика Северная Осетия — Алания', '15'),
    (1, 'Республика Татарстан', '16'),
    (1, 'Республика Тыва', '17'),
    (1, 'Удмуртская Республика', '18'),
    (1, 'Республика Хакасия', '19'),
    (1, 'Чеченская Республика', '20'),
    (1, 'Чувашская Республика', '21'),
    (1, 'Алтайский край', '22'),
    (1, 'Забайкальский край', '75'),
    (1, 'Камчатский край', '41'),
    (1, 'Краснодарский край', '23'),
    (1, 'Красноярский край', '24'),
    (1, 'Пермский край', '59'),
    (1, 'Приморский край', '25'),
    (1, 'Ставропольский край', '26'),
    (1, 'Хабаровский край', '27'),
    (1, 'Амурская область', '28'),
    (1, 'Архангельская область', '29'),
    (1, 'Астраханская область', '30'),
    (1, 'Белгородская область', '31'),
    (1, 'Брянская область', '32'),
    (1, 'Владимирская область', '33'),
    (1, 'Волгоградская область', '34'),
    (1, 'Вологодская область', '35'),
    (1, 'Воронежская область', '36'),
    (1, 'Ивановская область', '37'),
    (1, 'Иркутская область', '38'),
    (1, 'Калининградская область', '39'),
    (1, 'Калужская область', '40'),
    (1, 'Кемеровская область', '42'),
    (1, 'Кировская область', '43'),
    (1, 'Костромская область', '44'),
    (1, 'Курганская область', '45'),
    (1, 'Курская область', '46'),
    (1, 'Ленинградская область', '47'),
    (1, 'Липецкая область', '48'),
    (1, 'Магаданская область', '49'),
    (1, 'Московская область', '50'),
    (1, 'Мурманская область', '51'),
    (1, 'Нижегородская область', '52'),
    (1, 'Новгородская область', '53'),
    (1, 'Новосибирская область', '54'),
    (1, 'Омская область', '55'),
    (1, 'Оренбургская область', '56'),
    (1, 'Орловская область', '57'),
    (1, 'Пензенская область', '58'),
    (1, 'Псковская область', '60'),
    (1, 'Ростовская область', '61'),
    (1, 'Рязанская область', '62'),
    (1, 'Самарская область', '63'),
    (1, 'Саратовская область', '64'),
    (1, 'Сахалинская область', '65'),
    (1, 'Свердловская область', '66'),
    (1, 'Смоленская область', '67'),
    (1, 'Тамбовская область', '68'),
    (1, 'Тверская область', '69'),
    (1, 'Томская область', '70'),
    (1, 'Тульская область', '71'),
    (1, 'Тюменская область', '72'),
    (1, 'Ульяновская область', '73'),
    (1, 'Челябинская область', '74'),
    (1, 'Ярославская область', '76'),
    (1, 'Москва', '77'),
    (1, 'Санкт-Петербург', '78'),
    (1, 'Севастополь', '92'),
    (1, 'Еврейская автономная область', '79'),
    (1, 'Ненецкий автономный округ', '83'),
    (1, 'Ханты-Мансийский автономный округ - Югра', '86'),
    (1, 'Чукотский автономный округ', '87'),
    (1, 'Ямало-Ненецкий автономный округ', '89');

CREATE TABLE addresses (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    country_id INT NOT NULL REFERENCES countries(id) ON DELETE RESTRICT,
    region INT NOT NULL REFERENCES regions(id) ON DELETE RESTRICT,
    city VARCHAR(100) NOT NULL,
    street VARCHAR(150) NOT NULL,
    house VARCHAR(20) NOT NULL,
    office VARCHAR(20) NOT NULL,
    postal_code VARCHAR(10) NOT NULL
);

CREATE TABLE organizations (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    -- название через @ (по аналогии с users, например: @id000202)
    short_name VARCHAR(20) NOT NULL UNIQUE,
    description VARCHAR(200),
    legal_address_id INT REFERENCES addresses(id) ON DELETE RESTRICT,
    org_type_id INT REFERENCES organizations_types(id),
    system_status_id INT REFERENCES organization_system_statuses(id),
    site_url VARCHAR(500),
    site_label VARCHAR(20)
);

CREATE INDEX idx_organizations_legal_address_id ON organizations(legal_address_id);

CREATE TABLE organization_members (
    user_id INT NOT NULL PRIMARY KEY REFERENCES users(id) ON DELETE RESTRICT,
    organization_id INT NOT NULL REFERENCES organizations(id) ON DELETE RESTRICT,
    -- cвободное название должности внутри организации
    -- используется исключительно для внутренней коммуникации
    -- никаких прав не добавляет
    title VARCHAR(30) NOT NULL,
    is_owner BOOLEAN DEFAULT FALSE,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
-- для того, чтобы внутри организации был один владелец
CREATE UNIQUE INDEX uq_one_owner_per_org ON organization_members(organization_id) WHERE is_owner;

CREATE TABLE organizations_bans (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    organization_id INT NOT NULL REFERENCES organizations(id) ON DELETE RESTRICT,
    reason VARCHAR(50) NOT NULL,
    banned_by INT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    banned_at TIMESTAMPTZ DEFAULT NOW(),
    expires_at TIMESTAMPTZ,
    lifted_at TIMESTAMPTZ,
    lifted_by INT REFERENCES users(id) ON DELETE RESTRICT
);
