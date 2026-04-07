CREATE DATABASE IF NOT EXISTS library_reservation;
USE library_reservation;

DROP TRIGGER IF EXISTS tr_review_after_insert;
DROP TRIGGER IF EXISTS tr_review_after_update;
DROP TRIGGER IF EXISTS tr_review_after_delete;
DROP PROCEDURE IF EXISTS update_book_rating;

DROP TABLE IF EXISTS Reservation;
DROP TABLE IF EXISTS Review;
DROP TABLE IF EXISTS Waiting_list;
DROP TABLE IF EXISTS Users;
DROP TABLE IF EXISTS Book_Rating;
DROP TABLE IF EXISTS Book;

CREATE TABLE Users (
	user_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
	email VARCHAR(100) UNIQUE NOT NULL,
	password VARCHAR(255) NOT NULL,
	first_name VARCHAR(50) NOT NULL,
	last_name VARCHAR(50) NOT NULL,
	is_admin BOOLEAN DEFAULT FALSE,
	phone VARCHAR(10),
	status INT DEFAULT 1 COMMENT '1 means active, 0 means inactive',
	create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
	update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE Book (
	book_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
	title VARCHAR(255) NOT NULL,
	author VARCHAR(50) NOT NULL,
	genre VARCHAR(50) NOT NULL,
	description TEXT NULL,          -- 新增字段
	quantity INT,
    held_quantity INT DEFAULT 0,
	available BOOLEAN DEFAULT TRUE,
	`path` VARCHAR(255)
);

-- 触发器：插入前
DELIMITER $$
CREATE TRIGGER trg_book_bi_set_available
    BEFORE INSERT ON Book
    FOR EACH ROW
BEGIN
    IF NEW.quantity IS NULL OR NEW.quantity <= 0 THEN
        SET NEW.quantity  = 0;
        SET NEW.available = FALSE;
    END IF;
END$$
DELIMITER ;

-- 触发器：更新前
DELIMITER $$
CREATE TRIGGER trg_book_bu_set_available
    BEFORE UPDATE ON Book
    FOR EACH ROW
BEGIN
    IF NEW.quantity IS NULL OR NEW.quantity <= 0 THEN
        SET NEW.quantity  = 0;
        SET NEW.available = FALSE;
    END IF;
END$$
DELIMITER ;

CREATE TABLE Waiting_list (
	wait_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
	user_id INT NOT NULL,
	book_id INT NOT NULL,
	join_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	notification_date TIMESTAMP NULL,
	is_notified BOOLEAN DEFAULT FALSE,
	status ENUM('waiting', 'notified', 'cancelled', 'fulfilled') DEFAULT 'waiting',
	FOREIGN KEY (user_id) REFERENCES Users(user_id),
	FOREIGN KEY (book_id) REFERENCES Book(book_id)
);

CREATE TABLE Review (
	review_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
	user_id INT NOT NULL,
	book_id INT NOT NULL,
	rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
	comment TEXT,
	review_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	FOREIGN KEY (user_id) REFERENCES Users(user_id),
	FOREIGN KEY (book_id) REFERENCES Book(book_id),
	INDEX idx_review_book (book_id),
	INDEX idx_review_user (user_id)
);

CREATE TABLE Book_Rating (
	book_id INT NOT NULL PRIMARY KEY,
	avg_rating DECIMAL(3,2) NULL,
	CONSTRAINT fk_book_rating_book FOREIGN KEY (book_id) REFERENCES Book(book_id)
);

CREATE TABLE Reservation (
	reservation_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
	user_id INT NOT NULL,
	book_id INT NOT NULL,
	reservation_date TIMESTAMP NULL,
	pickup_date TIMESTAMP NULL,
	due_date TIMESTAMP NULL,
	return_date TIMESTAMP NULL,
	status ENUM('reserved', 'borrowed', 'returned', 'cancelled') DEFAULT 'reserved',
	create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	update_time TIMESTAMP NULL,
	is_delete BOOLEAN DEFAULT FALSE,
	FOREIGN KEY (user_id) REFERENCES Users(user_id),
	FOREIGN KEY (book_id) REFERENCES Book(book_id)
);

DELIMITER $$
CREATE PROCEDURE update_book_rating(IN p_book_id INT)
BEGIN
    INSERT INTO Book_Rating (book_id, avg_rating)
    SELECT
        b.book_id,
        CASE WHEN COUNT(r.review_id) = 0 THEN NULL ELSE ROUND(AVG(r.rating), 2) END AS avg_rating
    FROM Book b
             LEFT JOIN Review r ON r.book_id = b.book_id
    WHERE b.book_id = p_book_id
    GROUP BY b.book_id
    ON DUPLICATE KEY UPDATE
        avg_rating = VALUES(avg_rating);
END$$

CREATE TRIGGER tr_review_after_insert
    AFTER INSERT ON Review
    FOR EACH ROW
BEGIN
    CALL update_book_rating(NEW.book_id);
END$$

CREATE TRIGGER tr_review_after_update
    AFTER UPDATE ON Review
    FOR EACH ROW
BEGIN
    IF NEW.book_id <> OLD.book_id THEN
        CALL update_book_rating(OLD.book_id);
    END IF;
    CALL update_book_rating(NEW.book_id);
END$$

CREATE TRIGGER tr_review_after_delete
    AFTER DELETE ON Review
    FOR EACH ROW
BEGIN
    CALL update_book_rating(OLD.book_id);
END$$
DELIMITER ;

-- =========================
-- Sample Data
-- =========================

-- Users
INSERT INTO Users (user_id, email, password, first_name, last_name, is_admin) VALUES
	(1, '2797718037ljw@gmail.com', '0fcfcbbb420d3b6f81cd5a0d70a96579', 'Jingwei', 'Lin', FALSE),
	(2, '3058815969@qq.com', '0fcfcbbb420d3b6f81cd5a0d70a96579', 'Hanchen', 'Wang', FALSE),
	(3, 'admin@admin.com', '0fcfcbbb420d3b6f81cd5a0d70a96579', 'admin', 'admin', TRUE);

-- Books（含 description 全量 101 本）
INSERT INTO Book (book_id, title, author, genre, description, quantity, available, `path`) VALUES
	(1, 'The Hobbit', 'J.R.R. Tolkien', 'Fantasy', 'A reluctant hobbit, Bilbo Baggins, is swept into an epic quest with dwarves and a wizard to reclaim treasure guarded by the dragon Smaug; a warm, playful adventure about courage, home, and unexpected heroism.', 5, TRUE, 'The_Hobbit.jpg'),
	(2, 'The Lord of the Rings', 'J.R.R. Tolkien', 'Fantasy', 'An epic trilogy-scale tale of friendship and sacrifice as a small fellowship seeks to destroy the One Ring and defeat the dark power of Sauron.', 5, TRUE, 'The_Lord_of_the_Rings.jpg'),
	(3, 'Harry Potter and the Philosopher''s Stone', 'J.K. Rowling', 'Fantasy', 'A young boy discovers he''s a wizard and enters Hogwarts, where magic, friendship, and a dark mystery await.', 5, TRUE, 'Harry_Potter_and_the_Philosophers_Stone.jpg'),
	(4, 'A Game of Thrones', 'George R.R. Martin', 'Fantasy', 'Noble houses scheme, fight and form uneasy alliances in a richly detailed medieval world where power, honor, and survival collide.', 5, TRUE, 'A_Game_of_Thrones.jpg'),
	(5, 'The Name of the Wind', 'Patrick Rothfuss', 'Fantasy', 'Kvothe, a legendary musician and magician, tells the story of his extraordinary life, from humble beginnings to mythic exploits.', 5, TRUE, 'The_Name_of_the_Wind.jpg'),
	(6, 'Mistborn: The Final Empire', 'Brandon Sanderson', 'Fantasy', 'In a world ruled by an immortal tyrant, a band of thieves with metal-powered abilities plans a daring rebellion to topple an empire.', 5, TRUE, 'Mistborn_The_Final_Empire.jpg'),
	(7, 'The Lion, the Witch and the Wardrobe', 'C.S. Lewis', 'Fantasy', 'Four siblings enter the magical land of Narnia and join forces with Aslan to defeat the White Witch and end her endless winter.', 5, TRUE, 'The_Lion_the_Witch_and_the_Wardrobe.jpg'),
	(8, 'American Gods', 'Neil Gaiman', 'Fantasy', 'A road-trip narrative where ancient gods and modern deities clash across America as an ex-confronts his past and the strange new world around him.', 5, TRUE, 'American_Gods.jpg'),
	(9, 'Eragon', 'Christopher Paolini', 'Fantasy', 'A farm boy bonds with a dragon egg and is thrust into a conflict against a dark empire, beginning his transformation into a dragon rider.', 5, TRUE, 'Eragon.jpg'),
	(10, 'The Priory of the Orange Tree', 'Samantha Shannon', 'Fantasy', 'A sprawling, stand-alone epic of queens, dragon riders, and ancient prophecies that reshapes a world on the brink of war.', 5, TRUE, 'The_Priory_of_the_Orange_Tree.jpg'),

	(11, 'Dune', 'Frank Herbert', 'Science Fiction', 'On the desert planet Arrakis, political intrigue, spice, and prophecy intersect as young Paul Atreides confronts destiny and the future of humanity.', 5, TRUE, 'Dune.jpg'),
	(12, 'Neuromancer', 'William Gibson', 'Science Fiction', 'A washed-up hacker is hired for one last job that plunges him into cyberspace, corporate espionage, and a gritty cyberpunk underworld.', 5, TRUE, 'Neuromancer.jpg'),
	(13, 'Foundation', 'Isaac Asimov', 'Science Fiction', 'Hari Seldon''s science of psychohistory predicts the fall of a galactic empire and sets in motion a plan to preserve knowledge for future generations.', 5, TRUE, 'Foundation.jpg'),
	(14, 'Snow Crash', 'Neal Stephenson', 'Science Fiction', 'Fast-paced cyberpunk satire where a hacker-and-pizza-deliveryman confronts a dangerous new virtual drug that threatens both the metaverse and reality.', 5, TRUE, 'Snow_Crash.jpg'),
	(15, 'Ender''s Game', 'Orson Scott Card', 'Science Fiction', 'A child prodigy is trained through increasingly brutal war games to become humanity''s best hope against an alien threat.', 5, TRUE, 'Enders_Game.jpg'),
	(16, 'The Martian', 'Andy Weir', 'Science Fiction', 'Stranded on Mars, an astronaut uses science, ingenuity, and stubborn humor to survive while hoping for rescue.', 5, TRUE, 'The_Martian.jpg'),
	(17, 'Hyperion', 'Dan Simmons', 'Science Fiction', 'A pilgrimage of disparate travelers unfolds through interwoven tales as they approach a mysterious, time-bending world and its lethal landmark.', 5, TRUE, 'Hyperion.jpg'),
	(18, 'The Left Hand of Darkness', 'Ursula K. Le Guin', 'Science Fiction', 'An envoy navigates the complex politics and gender-fluid culture of an alien world while exploring themes of identity and empathy.', 5, TRUE, 'The_Left_Hand_of_Darkness.jpg'),
	(19, 'Ready Player One', 'Ernest Cline', 'Science Fiction', 'A pop-culture-fueled treasure hunt inside a vast virtual reality pits competitors against each other in a bid for wealth and control.', 5, TRUE, 'Ready_Player_One.jpg'),
	(20, 'The Three-Body Problem', 'Liu Cixin', 'Science Fiction', 'First contact with an alien civilization triggers a tense interplay of physics, politics, and the survival of humankind.', 5, TRUE, 'The_Three_Body_Problem.jpg'),

	(21, 'The Hound of the Baskervilles', 'Arthur Conan Doyle', 'Mystery', 'Sherlock Holmes investigates a legendary phantom hound and a family curse on the eerie English moors.', 5, TRUE, 'The_Hound_of_the_Baskervilles.jpg'),
	(22, 'Murder on the Orient Express', 'Agatha Christie', 'Mystery', 'Hercule Poirot must untangle motive and opportunity aboard a snowbound train where every passenger may be a suspect.', 5, TRUE, 'Murder_on_the_Orient_Express.jpg'),
	(23, 'The Girl with the Dragon Tattoo', 'Stieg Larsson', 'Mystery', 'A journalist and an extraordinary hacker join forces to unearth long-buried secrets in a wealthy family''s shadowy past.', 5, TRUE, 'The_Girl_with_the_Dragon_Tattoo.jpg'),
	(24, 'Gone Girl', 'Gillian Flynn', 'Mystery', 'A marriage dissolves into manipulation, media frenzy, and twists as a husband becomes prime suspect in his wife''s disappearance.', 5, TRUE, 'Gone_Girl.jpg'),
	(25, 'Big Little Lies', 'Liane Moriarty', 'Mystery', 'Suburban rivalries, secrets, and lies escalate toward a shocking confrontation at a school fundraiser.', 5, TRUE, 'Big_Little_Lies.jpg'),
	(26, 'In the Woods', 'Tana French', 'Mystery', 'A detective''s childhood trauma haunts him as he investigates a brutal murder that may link back to his past.', 5, TRUE, 'In_the_Woods.jpg'),
	(27, 'The Silent Patient', 'Alex Michaelides', 'Mystery', 'A psychotherapist tries to unravel why a celebrated painter shot her husband and then refused to speak afterward.', 5, TRUE, 'The_Silent_Patient.jpg'),
	(28, 'The Da Vinci Code', 'Dan Brown', 'Mystery', 'A symbologist and cryptologist chase clues through art, religion, and history as they unravel a hidden conspiracy.', 5, TRUE, 'The_Da_Vinci_Code.jpg'),
	(29, 'Still Life', 'Louise Penny', 'Mystery', 'Chief Inspector Gamache investigates a suspicious death in a small Canadian village, revealing layered motives and human kindness.', 5, TRUE, 'Still_Life.jpg'),
	(30, 'The No. 1 Ladies'' Detective Agency', 'Alexander McCall Smith', 'Mystery', 'Gentle, character-driven mysteries follow Botswana''s most endearing private investigator as she solves human-scale cases with warmth and wit.', 5, TRUE, 'The_No_1_Ladies_Detective_Agency.jpg'),

	(31, 'Pride and Prejudice', 'Jane Austen', 'Romance', 'Witty social satire and romance follow Elizabeth Bennet as she navigates manners, misunderstandings, and matters of the heart.', 5, TRUE, 'Pride_and_Prejudice.jpg'),
	(32, 'Outlander', 'Diana Gabaldon', 'Romance', 'A time-traveling nurse from WWII is swept into 18th-century Scotland where passion, danger, and history collide.', 5, TRUE, 'Outlander.jpg'),
	(33, 'Me Before You', 'Jojo Moyes', 'Romance', 'A caregiver forms an unexpected bond with a man facing life-changing disability, forcing both to confront love and hard choices.', 5, TRUE, 'Me_Before_You.jpg'),
	(34, 'The Notebook', 'Nicholas Sparks', 'Romance', 'A lifetime love story recalled through memory, devotion, and the struggle to preserve connection as ages advance.', 5, TRUE, 'The_Notebook.jpg'),
	(35, 'Twilight', 'Stephenie Meyer', 'Romance', 'A teenage girl falls for a mysterious vampire, igniting a romance complicated by danger, family, and immortality.', 5, TRUE, 'Twilight.jpg'),
	(36, 'The Kiss Quotient', 'Helen Hoang', 'Romance', 'A woman hires a professional to teach her dating skills, and a pragmatic arrangement grows into a surprising, tender romance.', 5, TRUE, 'The_Kiss_Quotient.jpg'),
	(37, 'It Ends with Us', 'Colleen Hoover', 'Romance', 'A courageous novel about love, domestic abuse, and the painful choices one must make to break destructive cycles.', 5, TRUE, 'It_Ends_with_Us.jpg'),
	(38, 'Beach Read', 'Emily Henry', 'Romance', 'Rival authors with writer''s block swap genres for the summer and discover themselves (and each other) in unexpected ways.', 5, TRUE, 'Beach_Read.jpg'),
	(39, 'Red, White & Royal Blue', 'Casey McQuiston', 'Romance', 'A romantic comedy about the First Son of the U.S. and a British prince whose impossible relationship becomes public and political.', 5, TRUE, 'Red_White_and_Royal_Blue.jpg'),
	(40, 'People We Meet on Vacation', 'Emily Henry', 'Romance', 'Two best friends'' annual trips hide deeper feelings that reemerge as they revisit places and memories together.', 5, TRUE, 'People_We_Meet_on_Vacation.jpg'),

	(41, 'The Book Thief', 'Markus Zusak', 'Historical Fiction', 'Told from Death''s perspective, a young girl in Nazi Germany discovers the power of books and words to resist despair.', 5, TRUE, 'The_Book_Thief.jpg'),
	(42, 'All the Light We Cannot See', 'Anthony Doerr', 'Historical Fiction', 'Interwoven lives of a blind French girl and a German boy illuminate human resilience amid World War II''s devastations.', 5, TRUE, 'All_the_Light_We_Cannot_See.jpg'),
	(43, 'The Nightingale', 'Kristin Hannah', 'Historical Fiction', 'Two sisters in occupied France each choose a different path of resistance and sacrifice during the horrors of war.', 5, TRUE, 'The_Nightingale.jpg'),
	(44, 'Wolf Hall', 'Hilary Mantel', 'Historical Fiction', 'A vivid political portrait of Thomas Cromwell''s rise within Henry VIII''s turbulent court, told with nuance and dark wit.', 5, TRUE, 'Wolf_Hall.jpg'),
	(45, 'The Tattooist of Auschwitz', 'Heather Morris', 'Historical Fiction', 'A moving, human-scale love story that emerges amid the horrors of a concentration camp.', 5, TRUE, 'The_Tattooist_of_Auschwitz.jpg'),
	(46, 'A Gentleman in Moscow', 'Amor Towles', 'Historical Fiction', 'Confined to a grand hotel, a count builds a rich, quiet life that touches others across decades of upheaval.', 5, TRUE, 'A_Gentleman_in_Moscow.jpg'),
	(47, 'War and Peace', 'Leo Tolstoy', 'Historical Fiction', 'Tolstoy''s sweeping epic traces Russian society, love, and warfare during Napoleon''s invasion, with philosophical depth and human detail.', 5, TRUE, 'War_and_Peace.jpg'),
	(48, 'The Pillars of the Earth', 'Ken Follett', 'Historical Fiction', 'A medieval saga of ambition, love, and rivalry set around the building of a cathedral and the lives it shapes.', 5, TRUE, 'The_Pillars_of_the_Earth.jpg'),
	(49, 'Beloved', 'Toni Morrison', 'Historical Fiction', 'A haunting, poetic novel about a mother''s past, memory, and the legacy of slavery that refuses to be buried.', 5, TRUE, 'Beloved.jpg'),
	(50, 'The Paris Library', 'Janet Skeslien Charles', 'Historical Fiction', 'Inspired by true events, librarians in occupied Paris resist tyranny through courage, books, and quiet acts of defiance.', 5, TRUE, 'The_Paris_Library.jpg'),

	(51, 'Dracula', 'Bram Stoker', 'Horror', 'An epistolary horror classic that chronicles the hunt for a vampire who threatens innocent lives and Victorian stability.', 5, TRUE, 'Dracula.jpg'),
	(52, 'Frankenstein', 'Mary Shelley', 'Horror', 'A scientist''s attempt to create life spirals into tragedy, raising enduring questions about responsibility, ambition, and the nature of humanity.', 5, TRUE, 'Frankenstein.jpg'),
	(53, 'The Shining', 'Stephen King', 'Horror', 'Isolation and supernatural menace unspool in an empty hotel, driving a father toward madness and danger for his family.', 5, TRUE, 'The_Shining.jpg'),
	(54, 'It', 'Stephen King', 'Horror', 'A town''s children face an ancient, shapeshifting evil that preys on children and returns decades later to test their courage.', 5, TRUE, 'It.jpg'),
	(55, 'Mexican Gothic', 'Silvia Moreno-Garcia', 'Horror', 'A gothic mystery in a decaying mansion where a socialite uncovers dark family secrets and creeping supernatural rot.', 5, TRUE, 'Mexican_Gothic.jpg'),
	(56, 'House of Leaves', 'Mark Z. Danielewski', 'Horror', 'An experimental, labyrinthine horror about a house that is larger on the inside than the outside, blending narrative layers and psychological dread.', 5, TRUE, 'House_of_Leaves.jpg'),
	(57, 'The Haunting of Hill House', 'Shirley Jackson', 'Horror', 'A classic ghost story exploring grief, trauma, and the ambiguous nature of haunted spaces.', 5, TRUE, 'The_Haunting_of_Hill_House.jpg'),
	(58, 'Bird Box', 'Josh Malerman', 'Horror', 'In a world where sight can bring madness and death, survivors must navigate an apocalyptic landscape while blindfolded and hunted by unseen terror.', 5, TRUE, 'Bird_Box.jpg'),
	(59, 'Something Wicked This Way Comes', 'Ray Bradbury', 'Horror', 'A dark carnival arrives to a small town, tempting two boys with nightmarish bargains and lessons about growing up.', 5, TRUE, 'Something_Wicked_This_Way_Comes.jpg'),
	(60, 'Hell House', 'Richard Matheson', 'Horror', 'Investigators confront a notorious, malevolent mansion in a tense study of fear, faith, and psychological collapse.', 5, TRUE, 'Hell_House.jpg'),

	(61, 'The Bourne Identity', 'Robert Ludlum', 'Thriller', 'An amnesiac man searches for his identity while evading assassins and uncovering a lethal past as a trained operative.', 5, TRUE, 'The_Bourne_Identity.jpg'),
	(62, 'The Girl on the Train', 'Paula Hawkins', 'Thriller', 'A commuter becomes entangled in a missing-person investigation as memory, obsession, and unreliable perception complicate the truth.', 5, TRUE, 'The_Girl_on_the_Train.jpg'),
	(63, 'The Reversal', 'Michael Connelly', 'Thriller', 'A legal thriller where a defense attorney switches roles to prosecute in a high-stakes retrial that questions justice and truth.', 5, TRUE, 'The_Reversal.jpg'),
	(64, 'Before I Go to Sleep', 'S.J. Watson', 'Thriller', 'A woman with daily amnesia rebuilds her life through fragments of memory and begins to suspect those around her cannot be trusted.', 5, TRUE, 'Before_I_Go_to_Sleep.jpg'),
	(65, 'Shutter Island', 'Dennis Lehane', 'Thriller', 'Marshals investigating a missing patient on a remote asylum island uncover psychological puzzles and unnerving revelations.', 5, TRUE, 'Shutter_Island.jpg'),
	(66, 'The Couple Next Door', 'Shari Lapena', 'Thriller', 'A missing-child mystery peels back the facades of suburban life, marriage, and trust.', 5, TRUE, 'The_Couple_Next_Door.jpg'),
	(67, 'Dark Places', 'Gillian Flynn', 'Thriller', 'A survivor revisits a horrific family massacre and confronts the tangled motives and secrets that shaped that night.', 5, TRUE, 'Dark_Places.jpg'),
	(68, 'I Am Watching You', 'Teresa Driscoll', 'Thriller', 'A travel encounter spirals into a tense missing-person case that pulls at the narrator''s conscience and memory.', 5, TRUE, 'I_Am_Watching_You.jpg'),
	(69, 'The Woman in the Window', 'A.J. Finn', 'Thriller', 'An agoraphobic woman believes she witnesses a crime, but her reliability becomes dangerously uncertain.', 5, TRUE, 'The_Woman_in_the_Window.jpg'),
	(70, 'Behind Closed Doors', 'B.A. Paris', 'Thriller', 'A picture-perfect marriage hides a terrifying reality of control and abuse that unravels behind closed doors.', 5, TRUE, 'Behind_Closed_Doors.jpg'),

	(71, 'The Hunger Games', 'Suzanne Collins', 'Young Adult', 'In a dystopian future, teenagers are forced into televised combat; one girl''s act of defiance ignites a revolution.', 5, TRUE, 'The_Hunger_Games.jpg'),
	(72, 'Divergent', 'Veronica Roth', 'Young Adult', 'In a society divided by virtues, a young woman discovers a dangerous identity that forces her to choose who she will be.', 5, TRUE, 'Divergent.jpg'),
	(73, 'The Fault in Our Stars', 'John Green', 'Young Adult', 'Two teens with serious illnesses find love, humor, and meaning as they face life''s fragility together.', 5, TRUE, 'The_Fault_in_Our_Stars.jpg'),
	(74, 'Eleanor & Park', 'Rainbow Rowell', 'Young Adult', 'A tender YA romance about two outsiders who bond over music, comics, and the ache of first love.', 5, TRUE, 'Eleanor_and_Park.jpg'),
	(75, 'Six of Crows', 'Leigh Bardugo', 'Young Adult', 'A ragtag crew of young criminals attempts a near-impossible heist in a richly imagined fantasy city.', 5, TRUE, 'Six_of_Crows.jpg'),
	(76, 'Throne of Glass', 'Sarah J. Maas', 'Young Adult', 'An assassin competes in a deadly contest and must navigate political intrigue, betrayal, and hidden magic.', 5, TRUE, 'Throne_of_Glass.jpg'),
	(77, 'A Court of Thorns and Roses', 'Sarah J. Maas', 'Young Adult', 'A Beauty-and-the-Beast–inspired fantasy where a mortal woman is drawn into the dangerous courts of the fae.', 5, TRUE, 'A_Court_of_Thorns_and_Roses.jpg'),
	(78, 'Legend', 'Marie Lu', 'Young Adult', 'A gripping YA thriller where a prodigy and a notorious criminal collide amid a dystopian republic''s tensions.', 5, TRUE, 'Legend.jpg'),
	(79, 'Cinder', 'Marissa Meyer', 'Young Adult', 'A cyborg Cinderella reimagining mixes science fiction, romance, and political conspiracy in a futuristic world.', 5, TRUE, 'Cinder.jpg'),
	(80, 'They Both Die at the End', 'Adam Silvera', 'Young Adult', 'Two strangers who learn they have one last day alive choose to meet and make that day matter.', 5, TRUE, 'They_Both_Die_at_the_End.jpg'),

	(81, 'Becoming', 'Michelle Obama', 'Non-Fiction', 'Michelle Obama''s intimate memoir traces her journey from childhood to the White House with honesty, warmth, and insight.', 5, TRUE, 'Becoming.jpg'),
	(82, 'Educated', 'Tara Westover', 'Non-Fiction', 'A memoir of one woman''s escape from a restrictive upbringing through education, self-determination, and resilience.', 5, TRUE, 'Educated.jpg'),
	(83, 'The Diary of a Young Girl', 'Anne Frank', 'Non-Fiction', 'The wartime diary of Anne Frank, a poignant firsthand account of hope, fear, and humanity in hiding.', 5, TRUE, 'The_Diary_of_a_Young_Girl.jpg'),
	(84, 'When Breath Becomes Air', 'Paul Kalanithi', 'Non-Fiction', 'A neurosurgeon reflects on life and mortality in this moving memoir written during his final year.', 5, TRUE, 'When_Breath_Becomes_Air.jpg'),
	(85, 'Born a Crime', 'Trevor Noah', 'Non-Fiction', 'Trevor Noah''s memoir blends humor and heart to tell stories of growing up mixed-race in apartheid South Africa.', 5, TRUE, 'Born_a_Crime.jpg'),
	(86, 'Steve Jobs', 'Walter Isaacson', 'Non-Fiction', 'A definitive biography detailing Steve Jobs''s life, vision, successes, flaws, and impact on technology and design.', 5, TRUE, 'Steve_Jobs.jpg'),
	(87, 'The Glass Castle', 'Jeannette Walls', 'Non-Fiction', 'A memoir of a fraught childhood, resilience, and a complicated family dynamic that ultimately shapes identity.', 5, TRUE, 'The_Glass_Castle.jpg'),
	(88, 'Eat, Pray, Love', 'Elizabeth Gilbert', 'Non-Fiction', 'A personal journey across Italy, India, and Indonesia as one woman seeks pleasure, devotion, and balance after a divorce.', 5, TRUE, 'Eat_Pray_Love.jpg'),
	(89, 'Into the Wild', 'Jon Krakauer', 'Non-Fiction', 'The story of Chris McCandless''s idealistic journey into the Alaskan wilderness and the complicated legacy it left behind.', 5, TRUE, 'Into_the_Wild.jpg'),
	(90, 'I Am Malala', 'Malala Yousafzai', 'Non-Fiction', 'The memoir of Malala Yousafzai, recounting her fight for girls'' education and her survival after an assassination attempt.', 5, TRUE, 'I_Am_Malala.jpg'),

	(91, 'The Iliad', 'Homer', 'Poetry Classic', 'Homer''s foundational epic dramatizes Achilles''s wrath and the human costs of war during the Trojan saga.', 5, TRUE, 'The_Iliad.jpg'),
	(92, 'The Odyssey', 'Homer', 'Poetry Classic', 'Odysseus''s long, cunning voyage home after the Trojan War is a timeless story of endurance, wit, and homecoming.', 5, TRUE, 'The_Odyssey.jpg'),
	(93, 'Leaves of Grass', 'Walt Whitman', 'Poetry Classic', 'Walt Whitman''s expansive, free-verse celebration of democracy, the self, nature, and the American spirit.', 5, TRUE, 'Leaves_of_Grass.jpg'),
	(94, 'The Waste Land', 'T.S. Eliot', 'Poetry Classic', 'T.S. Eliot''s modernist poem—fragmented, allusive, and haunting—explores disillusionment and the search for renewal.', 5, TRUE, 'The_Waste_Land.jpg'),
	(95, 'Paradise Lost', 'John Milton', 'Poetry Classic', 'Milton''s grand poetic retelling of the Fall explores defiance, free will, and the cosmic consequences of rebellion.', 5, TRUE, 'Paradise_Lost.jpg'),
	(96, 'The Divine Comedy', 'Dante Alighieri', 'Poetry Classic', 'Dante''s allegorical journey through Hell, Purgatory, and Paradise offers a richly imagined spiritual odyssey.', 5, TRUE, 'The_Divine_Comedy.jpg'),
	(97, 'Songs of Innocence and Experience', 'William Blake', 'Poetry Classic', 'William Blake''s paired collections contrast youthful wonder with worldly knowledge and moral tension.', 5, TRUE, 'Songs_of_Innocence_and_Experience.jpg'),
	(98, 'The Canterbury Tales', 'Geoffrey Chaucer', 'Poetry Classic', 'Chaucer''s lively collection of stories told by pilgrims offers comic, satiric, and moral portraits of medieval life.', 5, TRUE, 'The_Canterbury_Tales.jpg'),
	(99, 'Ariel', 'Sylvia Plath', 'Poetry Classic', 'Sylvia Plath''s intensely personal and powerful poems explore identity, transformation, and emotional extremes.', 5, TRUE, 'Ariel.jpg'),
	(100, 'The Raven and Other Poems', 'Edgar Allan Poe', 'Poetry Classic', 'Edgar Allan Poe''s dark, musical poems probe grief, longing, and the supernatural with memorable lyricism.', 5, TRUE, 'The_Raven_and_Other_Poems.jpg'),
	(101, 'Waiting List test', 'Test', 'Fantasy', 'Test', 1, True, 'The_Raven_and_Other_Poems.jpg');


-- Waiting list
INSERT INTO Waiting_list (wait_id, user_id, book_id, join_date, status) VALUES
    (1, 1, 1, CURRENT_TIMESTAMP, 'waiting');

-- Reviews（示例）
INSERT INTO Review (review_id, user_id, book_id, rating, comment, review_date) VALUES
	(1, 1, 1, 4, 'Great book!', CURRENT_TIMESTAMP),
	(2, 1, 1, 5, 'An epic adventure with rich world-building!', CURRENT_TIMESTAMP),
	(3, 2, 1, 3, 'Good story, but a bit slow in the middle.', CURRENT_TIMESTAMP),
	(4, 1, 2, 4, 'A classic fantasy masterpiece.', CURRENT_TIMESTAMP),
	(5, 2, 2, 5, 'Loved the depth of the characters and plot!', CURRENT_TIMESTAMP),
	(6, 1, 3, 4, 'Magical and engaging, great for all ages.', CURRENT_TIMESTAMP),
	(7, 2, 4, 2, 'Too complex for my taste.', CURRENT_TIMESTAMP),
	(8, 1, 5, 5, 'Kvothe’s story is captivating!', CURRENT_TIMESTAMP);

-- =========================
-- Initialize ratings once (batch)
-- =========================
INSERT INTO Book_Rating (book_id, avg_rating)
SELECT
    b.book_id,
    CASE WHEN COUNT(r.review_id) = 0 THEN NULL ELSE ROUND(AVG(r.rating), 2) END AS avg_rating
FROM Book b
         LEFT JOIN Review r ON r.book_id = b.book_id
GROUP BY b.book_id
ON DUPLICATE KEY UPDATE avg_rating = VALUES(avg_rating);

SELECT * FROM Book_Rating ORDER BY book_id;
