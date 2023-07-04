CREATE MATERIALIZED VIEW IF NOT EXISTS client_by_clientgroup_v2 AS
    SELECT * FROM clients
    WHERE id IS NOT NULL
    AND client_group_id IS NOT NULL
    PRIMARY KEY ((client_group_id), id);