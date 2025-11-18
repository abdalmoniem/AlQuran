Set WshShell = CreateObject("WScript.Shell")

WshShell.Run "pwsh -NoProfile -File ""start_desktop_headunit.ps1""", 0, False

Set WshShell = Nothing
