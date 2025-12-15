Set wshShell = CreateObject("WScript.Shell")
Set objArgs = WScript.Arguments

psCommand = "pwsh -NoProfile -File ""start_desktop_headunit.ps1"""

If objArgs.Count > 0 Then
    deviceParam = objArgs(0)
    psCommand = psCommand & " -device """ & deviceParam & """"
End If

wshShell.Run psCommand, 0, False

Set wshShell = Nothing
Set objArgs = Nothing
