This is a multi threaded program which will consist of three primary classes:

BellNote - Class to represent a note and its associated length.

Player - Class that represents a unique player for a musical note.
Each Player runs in its own thread and is responsible for playing one specific note.

Conductor - Class is responsible for loading and playing a song composed of musical notes.
It reads notes from a file, delegates playback to Note-specific Player objects,and coordinates 
synchronized audio playback.

When ran it will effectively play a song given through a text file.
