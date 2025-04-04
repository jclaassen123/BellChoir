import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 * The Conductor class is responsible for reading a song from a file and playing it.
 * It handles loading the notes from a file, creating player threads for each unique note,
 * and playing the song through the system's audio line.
 */
public class Conductor {
    private final AudioFormat af;
    private final Map<Note, Player> notePlayers;

    /**
     * Constructor for the Conductor class.
     *
     * @param af The AudioFormat used for audio playback.
     */
    Conductor(AudioFormat af) {
        this.af = af;
        this.notePlayers = new HashMap<>();
    }

    /**
     * The main method that starts the process of loading a song from a file and playing it.
     *
     * @param args The command-line arguments where the first argument is the filename of the song.
     * @throws Exception if there is an error loading or playing the song.
     */
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Usage: java Tone <filename>");
            return;
        }

        String filename = args[0];
        final AudioFormat af = new AudioFormat(Note.SAMPLE_RATE, 8, 1, true, false);
        Conductor t = new Conductor(af);

        List<BellNote> song = t.LoadNotes(filename);
        t.playSong(song);
    }

    /**
     * Plays the song by passing each note to its respective player.
     *
     * @param song The list of BellNote objects representing the song.
     * @throws LineUnavailableException if the audio line cannot be opened or used.
     */
    void playSong(List<BellNote> song) throws LineUnavailableException {
        try (final SourceDataLine line = AudioSystem.getSourceDataLine(af)) {
            line.open();
            line.start();

            for (BellNote bn : song) {
                Player player = getOrCreatePlayer(bn.note);
                player.playNote(line, bn);
            }

            line.drain();
        }
    }

    /**
     * Returns an existing player for the given note or creates a new one if it doesn't exist.
     *
     * @param note The musical note for which to get or create a player.
     * @return The Player object associated with the given note.
     */
    private Player getOrCreatePlayer(Note note) {
        if (!notePlayers.containsKey(note)) {
            // Create a new player thread if one does not exist
            Player player = new Player(note);
            notePlayers.put(note, player);
            new Thread(player).start();
        }
        return notePlayers.get(note);
    }

    /**
     * Loads a list of BellNote objects from a file.
     *
     * @param filename The name of the file containing the song.
     * @return A list of BellNote objects representing the song.
     */
    private List<BellNote> LoadNotes(String filename) {
        List<BellNote> song = new ArrayList<>();
        boolean isEmpty = true; // Track if the file is empty

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                isEmpty = false; // File has content

                String[] parts = line.split(" ");
                if (parts.length == 2) {
                    // Validate the note
                    Note note = validateNote(parts[0]);
                    if (note == null) {
                        System.err.println("Invalid note: " + parts[0] + " in line: " + line);
                        return null; // Stop processing and return null if the note is invalid
                    }

                    // Validate the note length
                    NoteLength length = validateNoteLength(parts[1]);
                    if (length == null) {
                        System.err.println("Invalid note length: " + parts[1] + " in line: " + line);
                        return null; // Stop processing and return null if the length is invalid
                    }

                    song.add(new BellNote(note, length));
                } else {
                    System.err.println("Invalid line format: " + line);
                    return null; // Stop processing and return null if the line format is invalid
                }
            }
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Error loading notes: " + e.getMessage());
            return null; // Return null if there's any error during file reading
        }

        // If the file was empty, print a message
        if (isEmpty) {
            System.err.println("The file is empty.");
            return null;
        }

        return song;
    }

    /**
     * Validates if the note is a valid enum value.
     *
     * @param noteStr the note string to validate
     * @return the corresponding Note enum, or null if invalid
     */
    private Note validateNote(String noteStr) {
        try {
            return Note.valueOf(noteStr);
        } catch (IllegalArgumentException e) {
            return null; // Return null if the note is invalid
        }
    }

    /**
     * Validates if the note length is a valid enum value.
     *
     * @param lengthStr the note length string to validate
     * @return the corresponding NoteLength enum, or null if invalid
     */
    private NoteLength validateNoteLength(String lengthStr) {
        switch (lengthStr) {
            case "1": return NoteLength.WHOLE;
            case "2": return NoteLength.HALF;
            case "4": return NoteLength.QUARTER;
            case "8": return NoteLength.EIGHTH;
            default: return null; // Return null if the length is invalid
        }
    }

    /**
     * Player class that represents a unique player for a musical note.
     * The player is responsible for playing a note through the audio line.
     */
    private static class Player implements Runnable {
        private final Note note;

        /**
         * Constructor for the Player class.
         *
         * @param note The musical note this player will play.
         */
        Player(Note note) {
            this.note = note;
        }

        @Override
        public void run() {
            // The thread runs to manage the playback of a note, if needed.
        }

        /**
         * Plays a given note using the provided audio line and BellNote.
         *
         * @param line The SourceDataLine used to play the audio.
         * @param bn The BellNote object representing the note and its length.
         */
        void playNote(SourceDataLine line, BellNote bn) {
            // Play the note
            final int ms = Math.min(bn.length.timeMs(), Note.MEASURE_LENGTH_SEC * 1000);
            final int length = Note.SAMPLE_RATE * ms / 1000;
            line.write(bn.note.sample(), 0, length);
            line.write(Note.REST.sample(), 0, 50); // Short silence between notes
        }
    }

    /**
     * BellNote class to represent a note and its associated length.
     */
    private static class BellNote {
        final Note note;
        final NoteLength length;

        /**
         * Constructor for the BellNote class.
         *
         * @param note The musical note.
         * @param length The length of the note.
         */
        BellNote(Note note, NoteLength length) {
            this.note = note;
            this.length = length;
        }
    }

    /**
     * Enum representing different note lengths and their corresponding duration in milliseconds.
     */
    enum NoteLength {
        WHOLE(1.0f),
        HALF(0.5f),
        QUARTER(0.25f),
        EIGHTH(0.125f);

        private final int timeMs;

        /**
         * Constructor for the NoteLength enum.
         *
         * @param length The relative length of the note (1.0f for WHOLE, etc.).
         */
        private NoteLength(float length) {
            timeMs = (int)(length * Note.MEASURE_LENGTH_SEC * 1000);
        }

        /**
         * Returns the length of the note in milliseconds.
         *
         * @return The duration of the note in milliseconds.
         */
        public int timeMs() {
            return timeMs;
        }
    }

    /**
     * Enum representing musical notes with their corresponding frequencies and sample data.
     */
    enum Note {
        REST,
        A0, B0, C0, C0S, D0, D0S, E0, F0, F0S, G0, G0S, A1, B1, C1, C1S, D1, D1S, E1, F1, F1S, G1, G1S,
        A2, B2, C2, C2S, D2, D2S, E2, F2, F2S, G2, G2S, A3, B3, C3, C3S, D3, D3S, E3, F3, F3S, G3, G3S,
        A4, B4, C4, C4S, D4, D4S, E4, F4, F4S, G4, G4S, A5, B5, C5, C5S, D5, D5S, E5, F5, F5S, G5, G5S,
        A6, B6, C6, C6S, D6, D6S, E6, F6, F6S, G6, G6S, A7, B7, C7, C7S, D7, D7S, E7, F7, F7S, G7, G7S,
        A8, B8, C8, C8S, D8, D8S, E8, F8, F8S, G8, G8S, A9, B9;

        public static final int SAMPLE_RATE = 48 * 1024; // ~48KHz
        public static final int MEASURE_LENGTH_SEC = 1;

        private final double FREQUENCY_A4_HZ = 440.0d; // Frequency for A4
        private final double step_alpha = (2.0d * Math.PI) / SAMPLE_RATE;
        private final double MAX_VOLUME = 127.0d;
        private final byte[] sinSample = new byte[MEASURE_LENGTH_SEC * SAMPLE_RATE];

        /**
         * Constructor for the Note enum that calculates the sine wave for the note's frequency.
         */
        private Note() {
            int n = this.ordinal();
            if (n > 0) {
                final int halfStepsFromA4 = n - 44;
                final double freq = FREQUENCY_A4_HZ * Math.pow(2.0d, halfStepsFromA4 / 12.0d);
                final double sinStep = freq * step_alpha;
                for (int i = 0; i < sinSample.length; i++) {
                    sinSample[i] = (byte)(Math.sin(i * sinStep) * MAX_VOLUME);
                }
            }
        }

        /**
         * Returns the audio sample for this note.
         *
         * @return A byte array representing the sine wave for this note.
         */
        public byte[] sample() {
            return sinSample;
        }
    }
}
