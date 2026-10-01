Unicode true
RequestExecutionLevel user
SetCompressor /SOLID lzma
Name "Reseam Manager"
OutFile "${OUTPUT_DIR}/reseam-manager-${VERSION}-windows-x64.exe"
InstallDir "$LOCALAPPDATA\Programs\Reseam Manager"
InstallDirRegKey HKCU "Software\Reseam\Manager" "InstallDir"
Icon "icon.ico"
UninstallIcon "icon.ico"
VIProductVersion "${VERSION}.0"
VIAddVersionKey "ProductName" "Reseam Manager"
VIAddVersionKey "FileDescription" "Reseam Manager Setup"
VIAddVersionKey "ProductVersion" "${VERSION}"
VIAddVersionKey "FileVersion" "${VERSION}"
VIAddVersionKey "LegalCopyright" "Reseam"

!include "MUI2.nsh"
!include "FileFunc.nsh"
!include "LogicLib.nsh"
!include "x64.nsh"

Var NoShortcuts

!macro CheckStopped
    ${If} ${FileExists} "$INSTDIR\runtime\bin\javaw.exe"
        System::Call 'kernel32::CreateFileW(w "$INSTDIR\runtime\bin\javaw.exe", i 0x40000000, i 0, p 0, i 3, i 0, p 0) p .r0'
        ${If} $0 == -1
            MessageBox MB_OK|MB_ICONSTOP "Close Reseam Manager and check that its installation folder is writable, then try again." /SD IDOK
            SetErrorLevel 1
            Abort
        ${EndIf}
        System::Call 'kernel32::CloseHandle(p r0)'
    ${EndIf}
!macroend

!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_COMPONENTS
!insertmacro MUI_PAGE_INSTFILES
!define MUI_FINISHPAGE_RUN "$INSTDIR\Reseam Manager.exe"
!insertmacro MUI_PAGE_FINISH
!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES
!insertmacro MUI_LANGUAGE "English"

Function .onInit
    SetShellVarContext current
    ${IfNot} ${RunningX64}
        MessageBox MB_OK|MB_ICONSTOP "Reseam Manager requires 64-bit Windows." /SD IDOK
        SetErrorLevel 1
        Quit
    ${EndIf}
    ${GetParameters} $0
    ClearErrors
    ${GetOptions} $0 "/NoShortcuts" $1
    ${IfNot} ${Errors}
        StrCpy $NoShortcuts 1
    ${EndIf}
FunctionEnd

Section "Reseam Manager" Main
    SectionIn RO
    !insertmacro CheckStopped
    ClearErrors
    SetOutPath "$INSTDIR"
    RMDir /r "$INSTDIR\lib"
    RMDir /r "$INSTDIR\runtime"
    ${If} ${Errors}
        MessageBox MB_OK|MB_ICONSTOP "The previous installation could not be replaced. Close Reseam Manager and check that its installation folder is writable, then try again." /SD IDOK
        SetErrorLevel 1
        Abort
    ${EndIf}
    File /r "${APP_DIR}/*"
    ${If} ${Errors}
        MessageBox MB_OK|MB_ICONSTOP "Close Reseam Manager and run setup again." /SD IDOK
        SetErrorLevel 1
        Abort
    ${EndIf}
    WriteUninstaller "$INSTDIR\Uninstall.exe"
    WriteRegStr HKCU "Software\Reseam\Manager" "InstallDir" "$INSTDIR"
    ${If} $NoShortcuts == 1
        WriteRegDWORD HKCU "Software\Reseam\Manager" "Shortcuts" 0
    ${Else}
        WriteRegDWORD HKCU "Software\Reseam\Manager" "Shortcuts" 1
    ${EndIf}
    WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\ReseamManager" "DisplayName" "Reseam Manager"
    WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\ReseamManager" "DisplayVersion" "${VERSION}"
    WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\ReseamManager" "Publisher" "Reseam"
    WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\ReseamManager" "InstallLocation" "$INSTDIR"
    WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\ReseamManager" "DisplayIcon" "$INSTDIR\Reseam Manager.exe"
    WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\ReseamManager" "UninstallString" '"$INSTDIR\Uninstall.exe"'
    WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\ReseamManager" "QuietUninstallString" '"$INSTDIR\Uninstall.exe" /S'
    WriteRegDWORD HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\ReseamManager" "NoModify" 1
    WriteRegDWORD HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\ReseamManager" "NoRepair" 1
    ${If} $NoShortcuts != 1
        CreateDirectory "$SMPROGRAMS\Reseam Manager"
        CreateShortcut "$SMPROGRAMS\Reseam Manager\Reseam Manager.lnk" "$INSTDIR\Reseam Manager.exe"
        CreateShortcut "$SMPROGRAMS\Reseam Manager\Uninstall.lnk" "$INSTDIR\Uninstall.exe"
    ${EndIf}
    ${If} ${Errors}
        MessageBox MB_OK|MB_ICONSTOP "Setup could not register Reseam Manager or create its shortcuts. Check that your user folders are writable, then run setup again." /SD IDOK
        SetErrorLevel 1
        Abort
    ${EndIf}
SectionEnd

Section /o "Desktop shortcut" DesktopShortcut
    ${If} $NoShortcuts != 1
        CreateShortcut "$DESKTOP\Reseam Manager.lnk" "$INSTDIR\Reseam Manager.exe"
        ${If} ${Errors}
            MessageBox MB_OK|MB_ICONSTOP "Setup could not create the desktop shortcut. Check that your desktop folder is writable, then run setup again." /SD IDOK
            SetErrorLevel 1
            Abort
        ${EndIf}
    ${EndIf}
SectionEnd

Section "Uninstall"
    SetShellVarContext current
    !insertmacro CheckStopped
    SetOutPath "$TEMP"
    ClearErrors
    RMDir /r "$INSTDIR\lib"
    RMDir /r "$INSTDIR\runtime"
    Delete "$INSTDIR\Reseam Manager.exe"
    Delete "$INSTDIR\icon.ico"
    ${If} ${Errors}
        MessageBox MB_OK|MB_ICONSTOP "Some app files could not be removed. Close Reseam Manager and run the uninstaller again." /SD IDOK
        SetErrorLevel 1
        Abort
    ${EndIf}
    Delete "$INSTDIR\Uninstall.exe"
    RMDir "$INSTDIR"
    ReadRegDWORD $0 HKCU "Software\Reseam\Manager" "Shortcuts"
    ${If} $0 == 1
        Delete "$SMPROGRAMS\Reseam Manager\Reseam Manager.lnk"
        Delete "$SMPROGRAMS\Reseam Manager\Uninstall.lnk"
        RMDir "$SMPROGRAMS\Reseam Manager"
        Delete "$DESKTOP\Reseam Manager.lnk"
    ${EndIf}
    DeleteRegKey HKCU "Software\Reseam\Manager"
    DeleteRegKey HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\ReseamManager"
SectionEnd
