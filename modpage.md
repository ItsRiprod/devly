[![Discord](https://img.shields.io/discord/1486567272792457319?logo=discord&logoColor=white&label=Discord&color=5865F2)](https://discord.hexcodec.com/) [![CurseForge](https://img.shields.io/curseforge/dt/1448311?logo=curseforge&logoColor=white&label=CurseForge&color=F16436)][![GitHub](https://img.shields.io/badge/GitHub-Source-181717?logo=github&logoColor=white)](https://github.com/itsriprod/devly)
# Devly
<p style="font-size:12px;font-style:italic;color:#4eb7fd">{ <a style="font-size:12px;font-style:italic;color:#4eb7fd" href="https://www.curseforge.com/hytale/mods/patchly">Powered by Patchly</a> }</p>

A simple to-the-point addon for Patchly
It provides the ability for mod developers to easily convert to `.patch` files and edit existing `.put` and `.patch` files directly from the asset editor!

<div style="color:#FF5555">Warning: Devly is Experimental. Ensure you have a backup of your mod before running any commands</div>

# Usage
## 1: Override an existing file
*This is the "classic" way of overriding hytale files*
![Override](https://media.modifold.com/projects/VsC9Ag/Screencast_20260825_233533.webp)
## 2: Edit your desired fields
*Anything, really. Arrays, strings, sub-assets, etc*
![Edit](https://media.modifold.com/projects/VsC9Ag/Screencast_20260825_233652.webp)
## 3: Run the Minify Command
`/devly minify --all`

*you have other options to minify specific files, minify a pack, etc*

<div style="color:#FFAA55;font-style:bold">Ensure you make a backup just in case</div>

![Minify](https://media.modifold.com/projects/VsC9Ag/Screencast_20260825_234056.webp)
## 4: Watch as your asset gets converted to `.patch`
![File](https://media.modifold.com/projects/VsC9Ag/Screencast_20260825_234323.webp)
## 5: Setup the Monitor so you can still edit your asset
*You can also edit the .patch directly. This just makes it more convenient*
![Monitor](https://media.modifold.com/projects/VsC9Ag/Screencast_20260825_234423.webp)
## 6: Update your assets in the `<yourpack>_Patchly` pack
*All of your edits will sync-back to the `.patch` to keep it updated
![Updates](https://media.modifold.com/projects/VsC9Ag/Screencast_20260825_234529.webp)


# Additional Options
### Ignoring a File
Add
```
{
    "$Devly": {
        "ignore": true
    }
}
```
to any asset to have it be ignored by Devly

# More to come!

Let me know your thoughts! Was this useful to you? Any feature requests? Would love to hear any feedback on this.