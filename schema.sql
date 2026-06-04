-- Create users table
CREATE TABLE users (
  id SERIAL PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  email VARCHAR(255) UNIQUE NOT NULL,
  password VARCHAR(255) NOT NULL,
  profile_photo VARCHAR(255),
  status VARCHAR(50) DEFAULT 'Disponible',
  connected BOOLEAN DEFAULT FALSE
);

-- Create conversations table
CREATE TABLE conversations (
  id SERIAL PRIMARY KEY,
  type VARCHAR(50) NOT NULL,
  group_name VARCHAR(255),
  creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create participants table
CREATE TABLE participants_conversation (
  user_id INT REFERENCES users(id) ON DELETE CASCADE,
  conversation_id INT REFERENCES conversations(id) ON DELETE CASCADE,
  role VARCHAR(50) DEFAULT 'MEMBER',
  PRIMARY KEY (user_id, conversation_id)
);

-- Create messages table
CREATE TABLE messages (
  id SERIAL PRIMARY KEY,
  conversation_id INT REFERENCES conversations(id) ON DELETE CASCADE,
  sender_id INT REFERENCES users(id) ON DELETE CASCADE,
  content TEXT NOT NULL,
  status VARCHAR(50) DEFAULT 'SENT',
  timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create attachments table
CREATE TABLE attachments (
  id SERIAL PRIMARY KEY,
  message_id INT REFERENCES messages(id) ON DELETE CASCADE,
  file_name VARCHAR(255) NOT NULL,
  file_type VARCHAR(100),
  file_path VARCHAR(255) NOT NULL,
  file_size DOUBLE PRECISION
);
