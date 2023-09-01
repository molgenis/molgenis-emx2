SELECT json_agg(json_build_object(
        'id', t.id,
        'sex', (select json_build_object('id', s.name, 'label', s.codesystem || ':' || s.code)
                from (select * from "GenderAtBirth" s where name = t.sex) as s),
        'diseaseCausalGenes',
        (select json_agg(json_build_object('id', s.name, 'label', s.codesystem || ':' || s.code))
         from (select * from "Genes" s where s.name = ANY (t."diseaseCausalGenes")) as s)
    )) as results
FROM (select * from "Individuals") as t