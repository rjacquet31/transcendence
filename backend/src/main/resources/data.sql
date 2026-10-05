INSERT INTO roles (role_name, description)

VALUES

('USER', 'Default user role'),

('ADMIN', 'Administrator role')

ON CONFLICT (role_name) DO NOTHING;


-- =========================================================
-- FOOT
-- =========================================================

INSERT INTO undercover_word_pair
(undercover_word, agents_word, category)
VALUES

('Messi', 'Ronaldo', 'foot'),
('PSG', 'OM', 'foot'),
('Real Madrid', 'Barcelona', 'foot'),
('Liverpool', 'Manchester United', 'foot'),
('Neymar', 'Mbappé', 'foot'),
('Gardien', 'Défenseur', 'foot'),
('Attaquant', 'Buteur', 'foot'),
('Penalty', 'Coup franc', 'foot'),
('Corner', 'Six mètres', 'foot'),
('Carton jaune', 'Carton rouge', 'foot'),
('Arbitre', 'VAR', 'foot'),
('Stade', 'Tribune', 'foot'),
('Championnat', 'Coupe', 'foot'),
('Ligue des champions', 'Ligue Europa', 'foot'),
('Ballon d''Or', 'Soulier d''Or', 'foot'),
('France', 'Brésil', 'foot'),
('Angleterre', 'Espagne', 'foot'),
('Milieu', 'Ailier', 'foot'),
('Dribble', 'Passe', 'foot'),
('But', 'Poteau', 'foot')

ON CONFLICT DO NOTHING;


-- =========================================================
-- JEUX VIDEO
-- =========================================================

INSERT INTO undercover_word_pair
(undercover_word, agents_word, category)
VALUES

('Mario', 'Luigi', 'jeux video'),
('Pokémon', 'Digimon', 'jeux video'),
('Minecraft', 'Terraria', 'jeux video'),
('Fortnite', 'PUBG', 'jeux video'),
('Call of Duty', 'Battlefield', 'jeux video'),
('FIFA', 'eFootball', 'jeux video'),
('PlayStation', 'Xbox', 'jeux video'),
('Nintendo', 'Sega', 'jeux video'),
('Zelda', 'Link', 'jeux video'),
('Kratos', 'Atreus', 'jeux video'),
('Dark Souls', 'Elden Ring', 'jeux video'),
('GTA', 'Watch Dogs', 'jeux video'),
('Assassin''s Creed', 'Prince of Persia', 'jeux video'),
('League of Legends', 'Dota', 'jeux video'),
('Valorant', 'Counter-Strike', 'jeux video'),
('Overwatch', 'Paladins', 'jeux video'),
('Resident Evil', 'Silent Hill', 'jeux video'),
('Sonic', 'Crash Bandicoot', 'jeux video'),
('Pac-Man', 'Space Invaders', 'jeux video'),
('Among Us', 'Fall Guys', 'jeux video')

ON CONFLICT DO NOTHING;


-- =========================================================
-- ANIME
-- =========================================================

INSERT INTO undercover_word_pair
(undercover_word, agents_word, category)
VALUES

('Naruto', 'Sasuke', 'anime'),
('Goku', 'Vegeta', 'anime'),
('Luffy', 'Zoro', 'anime'),
('Ichigo', 'Rukia', 'anime'),
('Eren', 'Armin', 'anime'),
('Gojo', 'Sukuna', 'anime'),
('Tanjiro', 'Nezuko', 'anime'),
('Deku', 'Bakugo', 'anime'),
('Light', 'L', 'anime'),
('Edward Elric', 'Alphonse Elric', 'anime'),
('Levi', 'Mikasa', 'anime'),
('Natsu', 'Gray', 'anime'),
('Saitama', 'Genos', 'anime'),
('Killua', 'Gon', 'anime'),
('Hisoka', 'Illumi', 'anime'),
('Inosuke', 'Zenitsu', 'anime'),
('Itachi', 'Madara', 'anime'),
('All Might', 'Endeavor', 'anime'),
('Shanks', 'Mihawk', 'anime'),
('Jotaro', 'Dio', 'anime')

ON CONFLICT DO NOTHING;


-- =========================================================
-- GENERAL
-- =========================================================

INSERT INTO undercover_word_pair
(undercover_word, agents_word, category)
VALUES

('Chat', 'Chien', 'general'),
('Pomme', 'Poire', 'general'),
('Pizza', 'Burger', 'general'),
('Café', 'Thé', 'general'),
('Été', 'Hiver', 'general'),
('Plage', 'Piscine', 'general'),
('Montagne', 'Colline', 'general'),
('Voiture', 'Moto', 'general'),
('Train', 'Métro', 'general'),
('Avion', 'Hélicoptère', 'general'),
('Cinéma', 'Théâtre', 'general'),
('Livre', 'Magazine', 'general'),
('École', 'Université', 'general'),
('Médecin', 'Infirmier', 'general'),
('Restaurant', 'Cafétéria', 'general'),
('Lit', 'Canapé', 'general'),
('Téléphone', 'Tablette', 'general'),
('Lunettes', 'Lentilles', 'general'),
('Forêt', 'Jungle', 'general'),
('Mer', 'Océan', 'general')

ON CONFLICT DO NOTHING;


-- =========================================================
-- TECH
-- =========================================================

INSERT INTO undercover_word_pair
(undercover_word, agents_word, category)
VALUES

('Java', 'C#', 'tech'),
('Python', 'JavaScript', 'tech'),
('React', 'Angular', 'tech'),
('Spring', 'Django', 'tech'),
('Git', 'GitHub', 'tech'),
('Docker', 'Kubernetes', 'tech'),
('Linux', 'Windows', 'tech'),
('Chrome', 'Firefox', 'tech'),
('PostgreSQL', 'MySQL', 'tech'),
('MongoDB', 'Redis', 'tech'),
('Frontend', 'Backend', 'tech'),
('CPU', 'GPU', 'tech'),
('RAM', 'SSD', 'tech'),
('Clavier', 'Souris', 'tech'),
('HTTP', 'HTTPS', 'tech'),
('API', 'SDK', 'tech'),
('Wi-Fi', 'Bluetooth', 'tech'),
('Cloud', 'Serveur', 'tech'),
('Algorithme', 'Programme', 'tech'),
('Intelligence artificielle', 'Machine Learning', 'tech')

ON CONFLICT DO NOTHING;


-- =========================================================
-- SF
-- =========================================================

INSERT INTO undercover_word_pair
(undercover_word, agents_word, category)
VALUES

('Star Wars', 'Star Trek', 'sf'),
('Alien', 'Predator', 'sf'),
('Robot', 'Android', 'sf'),
('Martien', 'Extraterrestre', 'sf'),
('Planète', 'Lune', 'sf'),
('Galaxie', 'Univers', 'sf'),
('Astronaute', 'Cosmonaute', 'sf'),
('Vaisseau spatial', 'Station spatiale', 'sf'),
('Téléportation', 'Voyage temporel', 'sf'),
('Laser', 'Plasma', 'sf'),
('Cyborg', 'Robot', 'sf'),
('Dystopie', 'Utopie', 'sf'),
('Trou noir', 'Étoile', 'sf'),
('Mars', 'Jupiter', 'sf'),
('Supernova', 'Explosion', 'sf'),
('Clonage', 'Mutation', 'sf'),
('Intelligence artificielle', 'Robotique', 'sf'),
('Voyage dans le temps', 'Paradoxe temporel', 'sf'),
('Empire galactique', 'Fédération', 'sf'),
('Soucoupe volante', 'OVNI', 'sf')

ON CONFLICT DO NOTHING;