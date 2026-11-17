-- Exclusivo do perfil demo. IDs estáveis permitem reaplicação sem duplicar dados.
INSERT INTO compara_preco.categoria (id, nome) VALUES
('10000000-0000-0000-0000-000000000001', 'Smartphones'),
('10000000-0000-0000-0000-000000000002', 'Notebooks')
ON CONFLICT (id) DO NOTHING;

INSERT INTO compara_preco.marca (id, nome) VALUES
('20000000-0000-0000-0000-000000000001', 'Apple'),
('20000000-0000-0000-0000-000000000002', 'Samsung')
ON CONFLICT (id) DO NOTHING;

INSERT INTO compara_preco.produto (id, categoria_id, marca_id, nome, modelo, descricao) VALUES
('30000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'Apple iPhone 16 Pro Max', 'iPhone 16 Pro Max', 'Produto demonstrativo para comparação de variantes.'),
('30000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002', 'Samsung Galaxy S24', 'Galaxy S24', 'Produto demonstrativo.'),
('30000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000001', 'Apple MacBook Air M3', 'MacBook Air M3', 'Notebook demonstrativo com memória informada nos atributos.')
ON CONFLICT (id) DO NOTHING;

INSERT INTO compara_preco.variante_produto (id, produto_id, armazenamento_gb, cor, condicao, atributos) VALUES
('40000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', 256, 'titânio natural', 'NOVO', '{"ram_gb":"8"}'),
('40000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000001', 512, 'titânio natural', 'NOVO', '{"ram_gb":"8"}'),
('40000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-000000000001', 256, 'titânio preto', 'NOVO', '{"ram_gb":"8"}'),
('40000000-0000-0000-0000-000000000004', '30000000-0000-0000-0000-000000000001', 256, 'titânio natural', 'USADO', '{"ram_gb":"8"}'),
('40000000-0000-0000-0000-000000000005', '30000000-0000-0000-0000-000000000002', 256, 'cinza', 'NOVO', '{"ram_gb":"8"}'),
('40000000-0000-0000-0000-000000000006', '30000000-0000-0000-0000-000000000003', 256, 'prateado', 'NOVO', '{"ram_gb":"8","tela_polegadas":"13"}')
ON CONFLICT (id) DO NOTHING;
