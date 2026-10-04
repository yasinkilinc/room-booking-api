-- btree_gist lets one exclusion constraint compare room_id (=) and time ranges (&&)
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE app_user (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL CHECK (role IN ('USER', 'ADMIN'))
);

CREATE TABLE room (
    id       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name     VARCHAR(80) NOT NULL,
    capacity INT         NOT NULL CHECK (capacity BETWEEN 1 AND 500),
    active   BOOLEAN     NOT NULL DEFAULT TRUE
);

CREATE UNIQUE INDEX room_name_unique ON room (lower(name));

CREATE TABLE booking (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    room_id    BIGINT       NOT NULL REFERENCES room (id),
    booked_by  VARCHAR(50)  NOT NULL REFERENCES app_user (username),
    title      VARCHAR(120) NOT NULL,
    attendees  INT          NOT NULL CHECK (attendees >= 1),
    starts_at  TIMESTAMPTZ  NOT NULL,
    ends_at    TIMESTAMPTZ  NOT NULL,
    status     VARCHAR(20)  NOT NULL CHECK (status IN ('CONFIRMED', 'CANCELLED')),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version    BIGINT       NOT NULL DEFAULT 0,
    CHECK (ends_at > starts_at),
    -- The final guard against double booking: two confirmed bookings of the same room
    -- can never overlap, even when requests arrive at the same moment.
    -- '[)' makes back-to-back bookings (10:00-11:00 and 11:00-12:00) legal.
    CONSTRAINT booking_no_overlap EXCLUDE USING gist (
        room_id WITH =,
        tstzrange(starts_at, ends_at, '[)') WITH &&
    ) WHERE (status = 'CONFIRMED')
);

CREATE INDEX booking_booked_by_starts_at ON booking (booked_by, starts_at);
