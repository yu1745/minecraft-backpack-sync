create database sync;
create table kit
(
    id       bigserial
        primary key,
    name     varchar(255) not null
        constraint kit_name_uindex
        unique,
    cooldown integer      not null,
    data     bytea        not null
);

comment on column kit.cooldown is '单位是分钟';

alter table kit
    owner to postgres;

create table kit_log
(
    id       bigserial
        primary key,
    username varchar(255)                        not null,
    time     timestamp default CURRENT_TIMESTAMP not null,
    kit_id   bigint                              not null
);

alter table kit_log
    owner to postgres;

create table login
(
    name         varchar(255) not null
        primary key,
    online       varchar(255) not null,
    last_login   varchar(255) not null,
    last_data_id bigint       not null
);

alter table login
    owner to postgres;

create table player
(
    id   bigserial
        primary key,
    name varchar(255)                        not null,
    time timestamp default CURRENT_TIMESTAMP not null,
    data bytea                               not null
);

alter table player
    owner to postgres;

create table tunnel_info
(
    id          bigserial
        primary key,
    from_server varchar(255) not null,
    from_world  varchar(255),
    from_x      integer      not null,
    from_y      integer      not null,
    from_z      integer      not null,
    to_server   varchar(255) not null,
    to_world    varchar(255) not null,
    to_x        integer      not null,
    to_y        integer      not null,
    to_z        integer      not null,
    active      boolean      not null,
    creator     varchar(255) not null
);

alter table tunnel_info
    owner to postgres;

create table tunnel_data
(
    id        bigserial not null
        constraint tunnel_pkey
        primary key,
    tunnel_id bigint    not null,
    data      bytea     not null
);

alter table tunnel_data
    owner to postgres;


