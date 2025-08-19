create table if not exists users (
    id bigint auto_increment primary key,
    username varchar(100) not null unique,
    password varchar(200) not null
);
create table if not exists user_roles (
    user_id bigint not null,
    role varchar(50) not null,
    constraint fk_user_roles_users foreign key (user_id) references users(id) on delete cascade
);