create table if not exists client_group(
    id uuid not null constraint pk_client_group primary key,
    name text not null
);