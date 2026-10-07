create table player (
    id           uuid primary key,
    display_name varchar(40) not null,
    created_at   timestamptz not null default now()
);

create table game (
    id              uuid primary key,
    variant         varchar(32) not null,
    status          varchar(16) not null check (status in ('OPEN', 'IN_PROGRESS', 'FINISHED')),
    black_player_id uuid references player (id),
    white_player_id uuid references player (id),
    ai_color        varchar(5) check (ai_color in ('BLACK', 'WHITE')),
    ai_difficulty   varchar(10),
    current_turn    varchar(5) not null check (current_turn in ('BLACK', 'WHITE')),
    board_snapshot  jsonb not null,
    result          varchar(10),
    result_reason   varchar(20),
    draw_offered_by varchar(5),
    version         int not null,
    created_at      timestamptz not null,
    updated_at      timestamptz not null,
    constraint game_ai_has_difficulty check ((ai_color is null) = (ai_difficulty is null)),
    constraint game_finished_has_result check ((status = 'FINISHED') = (result is not null))
);

-- Append-only move log: the source of truth for a game's position.
create table game_move (
    game_id   uuid not null references game (id) on delete cascade,
    ply       int not null check (ply > 0),
    from_sq   smallint not null check (from_sq between 1 and 32),
    path      smallint[] not null,
    captured  smallint[] not null,
    played_by uuid references player (id),
    played_at timestamptz not null,
    primary key (game_id, ply)
);

create index game_open_idx on game (created_at) where status = 'OPEN';
create index game_awaiting_ai_idx on game (updated_at) where status = 'IN_PROGRESS' and ai_color is not null;
create index game_black_player_idx on game (black_player_id);
create index game_white_player_idx on game (white_player_id);
