create schema dbo;

create table dbo.User(
    id int generated always as identity primary key,
    name VARCHAR(100) unique not null,
    password_validation VARCHAR(256) not null,
    balance int not null,
    rounds_played int not null DEFAULT 0,
    rounds_won int not null DEFAULT 0
);

CREATE TABLE dbo.tokens (
    id SERIAL PRIMARY KEY,
    token_validation VARCHAR(256) UNIQUE NOT NULL,
    user_id int REFERENCES dbo.user(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_used_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE dbo.LOBBY (
   id SERIAL PRIMARY KEY,
   name VARCHAR(255) NOT NULL,
   description TEXT,
   hostId INT NOT NULL REFERENCES dbo.User(id),
   rounds INT NOT NULL,
   ante INT NOT NULL DEFAULT 1,
   expectedPlayers INT NOT NULL,
   state VARCHAR(20) NOT NULL CHECK(state IN ('OPEN','FULL','CLOSED')),
   created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
   timeout BIGINT
);

CREATE TABLE dbo.LOBBY_PLAYER (
    lobbyid INT REFERENCES dbo.LOBBY(id) ON DELETE CASCADE,
    userid INT REFERENCES dbo.USER(id),
    PRIMARY KEY(lobbyid, userid)
);

CREATE TABLE dbo.MATCH (
    matchid SERIAL PRIMARY KEY,
    lobbyid INT REFERENCES dbo.LOBBY(id) ON DELETE CASCADE,
    currentRound INT DEFAULT 0,
    ante INT NOT NULL,
    totalRounds INT NOT NULL,
    status VARCHAR(20) NOT NULL CHECK(status IN ('ONGOING','FINISHED','NOT_STARTED'))
);

CREATE TABLE dbo.MATCH_PLAYER (
    matchId INTEGER REFERENCES dbo.MATCH(matchId) ON DELETE CASCADE,
    user_id INTEGER REFERENCES dbo.USER(id) ON DELETE CASCADE,
    PRIMARY KEY (matchId, user_id)
);

CREATE TABLE dbo.ROUND (
   matchId INTEGER REFERENCES dbo.MATCH(matchId) ON DELETE CASCADE,
   round_number INT NOT NULL,
   pot INT NOT NULL DEFAULT 0,
   winner_Id INT REFERENCES dbo.USER(id),
   curr_player_id INT REFERENCES dbo.USER(id) NULL,
   PRIMARY KEY (matchId, round_number)
);

CREATE TABLE dbo.TURN (
   id SERIAL PRIMARY KEY,
   matchid INTEGER NOT NULL,
   round_number INT NOT NULL,
   user_id INTEGER REFERENCES dbo.USER(id),
   rolls_json TEXT,
   final_hand VARCHAR(20),
   hand_rank VARCHAR(50),
   rolls_count INT DEFAULT 0,
   FOREIGN KEY (matchid, round_number) REFERENCES dbo.ROUND(matchId, round_number) ON DELETE CASCADE
);

CREATE TABLE dbo.APP_INVITE(
   id SERIAL PRIMARY KEY,
   inviterId INT REFERENCES dbo.USER(id),
   invite_code_hash VARCHAR(255) UNIQUE NOT NULL,
   usedAt TIMESTAMP WITH TIME ZONE NULL,
   createdAt TIMESTAMP WITH TIME ZONE NOT NULL
);
