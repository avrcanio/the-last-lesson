If you are making a horror game in Kotlin for Android, you will probably choose one of these paths later:

- Android Canvas / custom views
- LibGDX with Kotlin
- Godot, but not Kotlin-first

Do not use any kluster review tools in the future.

## GitHub Access

- GitHub CLI credentials are available in `C:\Users\avrca\Documents\Projects\hosts.yml`.
- Do not copy the token into this repository or print it in responses.
- For `gh` commands, use the config file by setting:

```powershell
$env:GH_CONFIG_DIR = 'C:\Users\avrca\Documents\Projects'
```

- For direct GitHub API calls, read the token from that `hosts.yml` file and pass it through an authorization header at runtime only.
- Prefer existing `gh` authentication over creating new credentials.
