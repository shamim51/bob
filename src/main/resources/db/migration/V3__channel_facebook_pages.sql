CREATE TABLE channel_facebook_pages (
    id SERIAL PRIMARY KEY,
    page_id VARCHAR NOT NULL,
    user_access_token VARCHAR NOT NULL,
    page_access_token VARCHAR NOT NULL,
    account_id INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    instagram_id VARCHAR,
    provider_name VARCHAR,
    reauthorization_required BOOLEAN DEFAULT FALSE NOT NULL
);

CREATE UNIQUE INDEX index_channel_facebook_pages_on_page_id_and_account_id
    ON channel_facebook_pages (page_id, account_id);
CREATE INDEX index_channel_facebook_pages_on_page_id ON channel_facebook_pages (page_id);
