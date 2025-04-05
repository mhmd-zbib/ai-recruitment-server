-- HireSync Database Schema
-- This file contains the core schema definitions for the HireSync application

-- Set the search path to public
SET search_path TO public;

-- Create extensions if they don't exist 
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Create schemas if they don't exist (normally just using public)
-- CREATE SCHEMA IF NOT EXISTS hiresync_data;

-- Create default sequences
CREATE SEQUENCE IF NOT EXISTS global_id_seq START WITH 1000;

-- Create base tables
-- 1. User Management
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_login_at TIMESTAMPTZ,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    account_non_expired BOOLEAN NOT NULL DEFAULT TRUE,
    account_non_locked BOOLEAN NOT NULL DEFAULT TRUE,
    credentials_non_expired BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- 2. Candidate Management
CREATE TABLE IF NOT EXISTS candidates (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(20),
    linkedin_url VARCHAR(255),
    github_url VARCHAR(255),
    website_url VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    current_position VARCHAR(100),
    current_company VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS candidate_skills (
    candidate_id UUID NOT NULL REFERENCES candidates(id) ON DELETE CASCADE,
    skill VARCHAR(50) NOT NULL,
    proficiency VARCHAR(20) NOT NULL DEFAULT 'INTERMEDIATE',
    years_of_experience INT,
    PRIMARY KEY (candidate_id, skill)
);

-- 3. Job Management
CREATE TABLE IF NOT EXISTS jobs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    requirements TEXT,
    location VARCHAR(100),
    salary_range VARCHAR(50),
    job_type VARCHAR(20) NOT NULL DEFAULT 'FULL_TIME',
    department VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    posted_at TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_by UUID REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS job_skills (
    job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    skill VARCHAR(50) NOT NULL,
    importance VARCHAR(20) NOT NULL DEFAULT 'REQUIRED',
    years_of_experience INT,
    PRIMARY KEY (job_id, skill)
);

-- 4. Application Management
CREATE TABLE IF NOT EXISTS applications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    candidate_id UUID NOT NULL REFERENCES candidates(id) ON DELETE CASCADE,
    job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    source VARCHAR(50) DEFAULT 'DIRECT',
    matching_score DECIMAL(5,2),
    ai_ranking INT,
    UNIQUE (candidate_id, job_id)
);

-- 5. Interview Management
CREATE TABLE IF NOT EXISTS interviews (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    application_id UUID NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    interviewer_id UUID NOT NULL REFERENCES users(id),
    scheduled_at TIMESTAMPTZ NOT NULL,
    duration_minutes INT NOT NULL DEFAULT 60,
    location VARCHAR(100),
    interview_type VARCHAR(20) NOT NULL DEFAULT 'VIDEO',
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS interview_feedback (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    interview_id UUID NOT NULL REFERENCES interviews(id) ON DELETE CASCADE,
    reviewer_id UUID NOT NULL REFERENCES users(id),
    technical_score INT CHECK (technical_score BETWEEN 1 AND 5),
    cultural_score INT CHECK (cultural_score BETWEEN 1 AND 5),
    overall_score INT CHECK (overall_score BETWEEN 1 AND 5),
    strengths TEXT,
    weaknesses TEXT,
    notes TEXT,
    recommendation VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_candidates_status ON candidates(status);
CREATE INDEX IF NOT EXISTS idx_applications_status ON applications(status);
CREATE INDEX IF NOT EXISTS idx_jobs_status ON jobs(status);
CREATE INDEX IF NOT EXISTS idx_interviews_scheduled_at ON interviews(scheduled_at);

-- Insert initial data
-- Default admin role
INSERT INTO roles (name, description) 
VALUES ('ROLE_ADMIN', 'Administrator role with full access')
ON CONFLICT (name) DO NOTHING;

INSERT INTO roles (name, description) 
VALUES ('ROLE_RECRUITER', 'Recruiter role with hiring abilities')
ON CONFLICT (name) DO NOTHING;

INSERT INTO roles (name, description) 
VALUES ('ROLE_INTERVIEWER', 'Interviewer role')
ON CONFLICT (name) DO NOTHING;

-- Create admin user if none exists
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM users WHERE username = 'admin') THEN
        INSERT INTO users (username, email, full_name, password_hash)
        VALUES (
            'admin', 
            'admin@hiresync.io',
            'Administrator',
            -- Default password: 'password' (hashed)
            '$2a$10$ymgMpnhMHCf4hCTRcXNJheLqTXIU3LFtKsF8XqRZlpRRnIQMBJxvG'
        );
        
        -- Assign admin role
        INSERT INTO user_roles (user_id, role_id)
        VALUES (
            (SELECT id FROM users WHERE username = 'admin'),
            (SELECT id FROM roles WHERE name = 'ROLE_ADMIN')
        );
    END IF;
END;
$$; 