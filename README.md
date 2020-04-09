
1. Download lyrics into data/ directory

```
cd data/
python download_lyrics.py
```

2. Index the files

```
mvn exec:java -Dexec.mainClass="com.distraction.lyrics.IndexFiles" -Dexec.args="-index index -docs data/"
```

3. Search and scroll

```
mvn exec:java -Dexec.mainClass="com.distraction.lyrics.SearchFiles" -Dexec.args="-index index/"
```