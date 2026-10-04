# Drawn avatars

The 24 avatar slots in `app/src/main/assets/avatar/` (`<theme>_<hero>_<rank>.webp`) are built from
[Free 25 Fantasy Character Asset Pack](https://cogabushi.itch.io/25-fantasy-character-asset-pack-rpg-vn-sprites-anime-style)
by cogabushi. Its licence allows use and modification in a game but forbids redistributing the images, even
edited, so the `.webp` files are git-ignored and never committed. Without them the app draws its vector
avatars.

## Rebuild

1. Download `free_fantasy_hero_25_character_pack.zip` from the page above (the download is free).
2. Unzip it so that `<ART_SRC>/pack/25_upper/101_1.png` exists (`D:\projects\art-src` on the main machine).
3. From `<ART_SRC>`, run the edit scripts. They cover open clothing so the art keeps the focus on studying:
   - `edit_104.py` and then `edit_104b.py`: closed side slits and long sleeves;
   - `edit_117.py`: closed shoulders and neckline;
   - `edit_119.py`: long sleeves.

   Each one writes to `pack/edited/`.
4. Build the slots straight into the app:

   ```
   ART_SRC=D:/projects/art-src python tools/avatars/make_avatars.py app/src/main/assets/avatar
   ```

`MAPPING` in `make_avatars.py` lists the character for each hero and rank, E to S.
