Unicode true
RequestExecutionLevel user
SilentInstall silent
AutoCloseWindow true
Name "Reseam Manager"
OutFile "${OUTPUT_FILE}"
Icon "icon.ico"

Section
    SetOutPath "$EXEDIR"
    ClearErrors
    ReadEnvStr $1 "LOCALAPPDATA"
    StrCpy $2 "$1\app.reseam.manager\cache\tmp"
    CreateDirectory "$2"
    IfErrors launchFailed
    ExecWait '"$EXEDIR\runtime\bin\javaw.exe" "-Djava.io.tmpdir=$2" -cp "$EXEDIR\lib\*" app.reseam.manager.MainKt' $0
    IfErrors launchFailed
    IntCmp $0 0 done
    ReadEnvStr $1 "LOCALAPPDATA"
    MessageBox MB_OK|MB_ICONSTOP "Reseam Manager stopped with an error. See $1\app.reseam.manager\logs\manager.log for details."
    SetErrorLevel $0
    Goto done
launchFailed:
    MessageBox MB_OK|MB_ICONSTOP "Reseam Manager could not start its bundled Java runtime. Reinstall the app."
    SetErrorLevel 1
done:
SectionEnd
