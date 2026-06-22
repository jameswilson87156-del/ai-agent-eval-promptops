CREATE TABLE IF NOT EXISTS prompt_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_key VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    scenario VARCHAR(128) NOT NULL,
    description VARCHAR(512),
    owner_team VARCHAR(128),
    status VARCHAR(32) NOT NULL DEFAULT 'Draft',
    current_version_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS prompt_version (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT NOT NULL,
    version_label VARCHAR(32) NOT NULL,
    template_text CLOB NOT NULL,
    -- Standard JSON array string of Prompt variables for H2 test compatibility.
    variables_json CLOB NOT NULL,
    output_format VARCHAR(32) NOT NULL DEFAULT 'JSON',
    status VARCHAR(32) NOT NULL DEFAULT 'Draft',
    author VARCHAR(128),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS eval_dataset (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dataset_key VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    scenario VARCHAR(128) NOT NULL,
    description VARCHAR(512),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS eval_case (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dataset_id BIGINT NOT NULL,
    case_key VARCHAR(80) NOT NULL,
    title VARCHAR(180) NOT NULL,
    input_text CLOB NOT NULL,
    expected_text CLOB NOT NULL,
    risk_level VARCHAR(32) NOT NULL DEFAULT 'normal',
    tags_json CLOB,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS score_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    case_id BIGINT NOT NULL,
    rule_type VARCHAR(40) NOT NULL,
    rule_value VARCHAR(512),
    expected_format VARCHAR(32),
    weight DECIMAL(6,2) NOT NULL DEFAULT 1.00,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    description VARCHAR(512),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS eval_run (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    run_key VARCHAR(80) NOT NULL UNIQUE,
    prompt_version_id BIGINT NOT NULL,
    dataset_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'queued',
    total_cases INT NOT NULL DEFAULT 0,
    passed_cases INT NOT NULL DEFAULT 0,
    failed_cases INT NOT NULL DEFAULT 0,
    risk_case_count INT NOT NULL DEFAULT 0,
    pass_rate DECIMAL(6,2) NOT NULL DEFAULT 0,
    average_latency_ms INT NOT NULL DEFAULT 0,
    started_at TIMESTAMP NULL,
    finished_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS eval_case_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    eval_run_id BIGINT NOT NULL,
    eval_case_id BIGINT NOT NULL,
    result_status VARCHAR(32) NOT NULL,
    score DECIMAL(6,2) NOT NULL,
    latency_ms INT NOT NULL,
    failure_reasons_json CLOB NOT NULL,
    output_summary VARCHAR(512),
    mock_output CLOB,
    explanation VARCHAR(1024),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS generation_trace (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    eval_run_id BIGINT,
    eval_case_id BIGINT,
    prompt_version_id BIGINT NOT NULL,
    input_summary VARCHAR(512),
    output_summary VARCHAR(512),
    latency_ms INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    error_reason VARCHAR(512),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
