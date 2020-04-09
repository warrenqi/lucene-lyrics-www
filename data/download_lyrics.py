import lyricsgenius

artists = ["AC/DC", "Ava Max", "Adele", "Alicia Keys"]

genius = lyricsgenius.Genius("TODO___PUT___TOKEN___HERE")

for artist in artists:
    artist_result = genius.search_artist(artist, max_songs=12, sort="popularity", get_full_info=False)
    for song in artist_result.songs:
        try:
            print("saving lyrics for: " + song.title)
            song.save_lyrics(extension="txt")
        except:
            print("exception at saving lyrics")
