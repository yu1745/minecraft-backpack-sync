create table tunnel
(
    id          bigint auto_increment
        primary key,
    from_server varchar(36) not null,
    from_world  varchar(36) null,
    from_x      int         not null,
    from_y      int         not null,
    from_z      int         not null,
    to_server   varchar(36) not null,
    to_world    varchar(36) not null,
    to_x        int         not null,
    to_y        int         not null,
    to_z        int         not null,
    active      tinyint(1)  not null
);

