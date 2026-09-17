-- Chatwoot conversation-slice tables (columns aligned with db/schema.rb)

CREATE TABLE accounts (
    id SERIAL PRIMARY KEY,
    name VARCHAR NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    locale INTEGER DEFAULT 0,
    domain VARCHAR(100),
    support_email VARCHAR(100),
    feature_flags BIGINT DEFAULT 0 NOT NULL,
    auto_resolve_duration INTEGER,
    limits JSONB DEFAULT '{}',
    custom_attributes JSONB DEFAULT '{}',
    status INTEGER DEFAULT 0,
    internal_attributes JSONB DEFAULT '{}' NOT NULL,
    settings JSONB DEFAULT '{}',
    feature_flags_ext_1 BIGINT DEFAULT 0 NOT NULL
);

CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    provider VARCHAR DEFAULT 'email' NOT NULL,
    uid VARCHAR DEFAULT '' NOT NULL,
    encrypted_password VARCHAR DEFAULT '' NOT NULL,
    name VARCHAR NOT NULL,
    display_name VARCHAR,
    email VARCHAR,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    pubsub_token VARCHAR,
    availability INTEGER DEFAULT 0,
    ui_settings JSONB DEFAULT '{}',
    custom_attributes JSONB DEFAULT '{}',
    type VARCHAR,
    message_signature TEXT
);

CREATE UNIQUE INDEX index_users_on_email ON users (email);
CREATE UNIQUE INDEX index_users_on_pubsub_token ON users (pubsub_token);
CREATE UNIQUE INDEX index_users_on_uid_and_provider ON users (uid, provider);

CREATE TABLE account_users (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT,
    user_id BIGINT,
    role INTEGER DEFAULT 0,
    inviter_id BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    active_at TIMESTAMP,
    availability INTEGER DEFAULT 0 NOT NULL,
    auto_offline BOOLEAN DEFAULT TRUE NOT NULL
);

CREATE UNIQUE INDEX uniq_user_id_per_account_id ON account_users (account_id, user_id);

CREATE TABLE inboxes (
    id SERIAL PRIMARY KEY,
    channel_id INTEGER NOT NULL,
    account_id INTEGER NOT NULL,
    name VARCHAR NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    channel_type VARCHAR,
    enable_auto_assignment BOOLEAN DEFAULT TRUE,
    greeting_enabled BOOLEAN DEFAULT FALSE,
    greeting_message VARCHAR,
    email_address VARCHAR,
    working_hours_enabled BOOLEAN DEFAULT FALSE,
    out_of_office_message VARCHAR,
    timezone VARCHAR DEFAULT 'UTC',
    enable_email_collect BOOLEAN DEFAULT TRUE,
    csat_survey_enabled BOOLEAN DEFAULT FALSE,
    allow_messages_after_resolved BOOLEAN DEFAULT TRUE,
    auto_assignment_config JSONB DEFAULT '{}',
    lock_to_single_conversation BOOLEAN DEFAULT FALSE NOT NULL,
    sender_name_type INTEGER DEFAULT 0 NOT NULL,
    business_name VARCHAR,
    csat_config JSONB DEFAULT '{}' NOT NULL
);

CREATE TABLE inbox_members (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    inbox_id INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX index_inbox_members_on_inbox_id_and_user_id ON inbox_members (inbox_id, user_id);

CREATE TABLE contacts (
    id SERIAL PRIMARY KEY,
    name VARCHAR DEFAULT '',
    email VARCHAR,
    phone_number VARCHAR,
    account_id INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    additional_attributes JSONB DEFAULT '{}',
    identifier VARCHAR,
    custom_attributes JSONB DEFAULT '{}',
    last_activity_at TIMESTAMP,
    contact_type INTEGER DEFAULT 0,
    middle_name VARCHAR DEFAULT '',
    last_name VARCHAR DEFAULT '',
    location VARCHAR DEFAULT '',
    country_code VARCHAR DEFAULT '',
    blocked BOOLEAN DEFAULT FALSE NOT NULL
);

CREATE TABLE contact_inboxes (
    id BIGSERIAL PRIMARY KEY,
    contact_id BIGINT,
    inbox_id BIGINT,
    source_id TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    hmac_verified BOOLEAN DEFAULT FALSE,
    pubsub_token VARCHAR
);

CREATE UNIQUE INDEX index_contact_inboxes_on_inbox_id_and_source_id ON contact_inboxes (inbox_id, source_id);

CREATE TABLE conversations (
    id SERIAL PRIMARY KEY,
    account_id INTEGER NOT NULL,
    inbox_id INTEGER NOT NULL,
    status INTEGER DEFAULT 0 NOT NULL,
    assignee_id INTEGER,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    contact_id BIGINT,
    display_id INTEGER NOT NULL,
    contact_last_seen_at TIMESTAMP,
    agent_last_seen_at TIMESTAMP,
    additional_attributes JSONB DEFAULT '{}',
    contact_inbox_id BIGINT,
    uuid UUID DEFAULT gen_random_uuid() NOT NULL,
    identifier VARCHAR,
    last_activity_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    team_id BIGINT,
    campaign_id BIGINT,
    snoozed_until TIMESTAMP,
    custom_attributes JSONB DEFAULT '{}',
    assignee_last_seen_at TIMESTAMP,
    first_reply_created_at TIMESTAMP,
    priority INTEGER,
    sla_policy_id BIGINT,
    waiting_since TIMESTAMP,
    cached_label_list TEXT,
    assignee_agent_bot_id BIGINT,
    ai_assignee_type VARCHAR,
    status_changed_at TIMESTAMP
);

CREATE UNIQUE INDEX index_conversations_on_account_id_and_display_id ON conversations (account_id, display_id);
CREATE UNIQUE INDEX index_conversations_on_uuid ON conversations (uuid);

CREATE TABLE conversation_display_id_counters (
    account_id INTEGER PRIMARY KEY,
    last_value INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE messages (
    id SERIAL PRIMARY KEY,
    content TEXT,
    account_id INTEGER NOT NULL,
    inbox_id INTEGER NOT NULL,
    conversation_id INTEGER NOT NULL,
    message_type INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    private BOOLEAN DEFAULT FALSE NOT NULL,
    status INTEGER DEFAULT 0,
    source_id TEXT,
    content_type INTEGER DEFAULT 0 NOT NULL,
    content_attributes JSON DEFAULT '{}',
    sender_type VARCHAR,
    sender_id BIGINT,
    external_source_ids JSONB DEFAULT '{}',
    additional_attributes JSONB DEFAULT '{}',
    processed_message_content TEXT,
    sentiment JSONB DEFAULT '{}'
);

CREATE INDEX index_messages_on_conversation_id ON messages (conversation_id);
CREATE INDEX index_messages_on_source_id ON messages (source_id);
