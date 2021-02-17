Type INIFile
	Field name$
	Field bank%
	Field bankOffset% = 0
	Field size%
End Type

Function ReadINILine$(file.INIFile)
	Local rdbyte%
	Local firstbyte% = True
	Local offset% = file\bankOffset
	Local bank% = file\bank
	Local retStr$ = ""
	
	rdbyte = PeekByte(bank,offset)
	While ((firstbyte) Lor ((rdbyte<>13) And (rdbyte<>10))) And (offset<file\size)
		rdbyte = PeekByte(bank,offset)
		If ((rdbyte<>13) And (rdbyte<>10)) Then
			firstbyte = False
			retStr=retStr+Chr(rdbyte)
		EndIf
		offset=offset+1
	Wend
	file\bankOffset = offset
	Return retStr
End Function

Function UpdateINIFile$(filename$)
	Local file.INIFile = Null
	
	For k.INIFile = Each INIFile
		If k\name = Lower(filename) Then
			file = k
			Exit
		EndIf
	Next
	
	If file=Null Then Return
	
	If file\bank<>0 Then FreeBank file\bank
	
	Local f% = ReadFile(file\name)
	Local fleSize% = 1
	
	While fleSize<FileSize(file\name)
		fleSize=fleSize*2
	Wend
	file\bank = CreateBank(fleSize)
	file\size = 0
	While Not Eof(f)
		PokeByte(file\bank,file\size,ReadByte(f))
		file\size=file\size+1
	Wend
	CloseFile(f)
End Function

Function GetINIString$(file$, section$, parameter$, defaultvalue$="")
	Local TemporaryString$ = ""
	Local lfile.INIFile = Null
	
	For k.INIFile = Each INIFile
		If k\name = Lower(file) Then
			lfile = k
			Exit
		EndIf
	Next
	
	If lfile = Null Then
		DebugLog "CREATE BANK FOR "+file
		lfile = New INIFile
		lfile\name = Lower(file)
		lfile\bank = 0
		UpdateINIFile(lfile\name)
	EndIf
	
	lfile\bankOffset = 0
	
	section = Lower(section)
	
	While lfile\bankOffset<lfile\size
		Local strtemp$ = ReadINILine(lfile)
		
		If Left(strtemp,1) = "[" Then
			strtemp$ = Lower(strtemp)
			If Mid(strtemp, 2, Len(strtemp)-2)=section Then
				Repeat
					TemporaryString = ReadINILine(lfile)
					If Lower(Trim(Left(TemporaryString, Max(Instr(TemporaryString, "=") - 1, 0)))) = Lower(parameter) Then
						Return Trim( Right(TemporaryString,Len(TemporaryString)-Instr(TemporaryString,"=")) )
					EndIf
				Until (Left(TemporaryString, 1) = "[") Lor (lfile\bankOffset>=lfile\size)
				Return defaultvalue
			EndIf
		EndIf
	Wend
	Return defaultvalue
End Function

Function GetINIInt%(file$, section$, parameter$, defaultvalue% = 0)
	Local txt$ = GetINIString(file$, section$, parameter$, defaultvalue)
	
	If Lower(txt) = "true" Then
		Return 1
	ElseIf Lower(txt) = "false"
		Return 0
	Else
		Return Int(txt)
	EndIf
End Function

Function GetINIFloat#(file$, section$, parameter$, defaultvalue# = 0.0)
	Return Float(GetINIString(file$, section$, parameter$, defaultvalue))
End Function

Function GetINIString2$(file$, start%, parameter$, defaultvalue$="")
	Local TemporaryString$ = ""
	Local f% = ReadFile(file)
	Local n%=0
	
	While Not Eof(f)
		Local strtemp$ = ReadLine(f)
		
		n=n+1
		If n=start Then 
			Repeat
				TemporaryString = ReadLine(f)
				If Lower(Trim(Left(TemporaryString, Max(Instr(TemporaryString, "=") - 1, 0)))) = Lower(parameter) Then
					CloseFile f
					Return Trim( Right(TemporaryString,Len(TemporaryString)-Instr(TemporaryString,"=")) )
				EndIf
			Until Left(TemporaryString, 1) = "[" Lor Eof(f)
			CloseFile f
			Return defaultvalue
		EndIf
	Wend
	CloseFile f	
	Return defaultvalue
End Function

Function GetINIInt2%(file$, start%, parameter$, defaultvalue$="")
	Local txt$ = GetINIString2(file$, start%, parameter$, defaultvalue$)
	
	If Lower(txt) = "true" Then
		Return 1
	ElseIf Lower(txt) = "false"
		Return 0
	Else
		Return Int(txt)
	EndIf
End Function

Function GetINISectionLocation%(file$, section$)
	Local Temp%
	Local f% = ReadFile(file)
	Local n%=0
	
	section = Lower(section)
	
	While Not Eof(f)
		Local strtemp$ = ReadLine(f)
		
		n=n+1
		If Left(strtemp,1) = "[" Then
			strtemp$ = Lower(strtemp)
			Temp = Instr(strtemp, section)
			If Temp>0 Then
				If Mid(strtemp, Temp-1, 1)="[" Lor Mid(strtemp, Temp-1, 1)="|" Then
					CloseFile f
					Return n
				EndIf
			EndIf
		EndIf
	Wend
	CloseFile f
End Function

Function PutINIValue%(file$, INI_sSection$, INI_sKey$, INI_sValue$)
	; Returns: True (Success) or False (Failed)
	INI_sSection = "[" + Trim$(INI_sSection) + "]"
	
	Local INI_sUpperSection$ = Upper$(INI_sSection)
	
	INI_sKey = Trim$(INI_sKey)
	INI_sValue = Trim$(INI_sValue)
	
	Local INI_sFilename$ = file$
	; Retrieve the INI Data (If it exists)
	Local INI_sContents$ = INI_FileToString(INI_sFilename)
	; (Re)Create the INI file updating/adding the SECTION, KEY And VALUE
	Local INI_bWrittenKey% = False
	Local INI_bSectionFound% = False
	Local INI_sCurrentSection$ = ""
	
	Local INI_lFileHandle% = WriteFile(INI_sFilename)
	
	If INI_lFileHandle = 0 Then Return False ; Create file failed!
	
	Local INI_lOldPos% = 1
	Local INI_lPos% = Instr(INI_sContents, Chr$(0))
	
	While (INI_lPos <> 0)
		Local INI_sTemp$ = Mid$(INI_sContents, INI_lOldPos, (INI_lPos - INI_lOldPos))
		
		If (INI_sTemp <> "") Then
			If Left$(INI_sTemp, 1) = "[" And Right$(INI_sTemp, 1) = "]" Then
				; Process SECTION
				If (INI_sCurrentSection = INI_sUpperSection) And (INI_bWrittenKey = False) Then
					INI_bWrittenKey = INI_CreateKey(INI_lFileHandle, INI_sKey, INI_sValue)
				EndIf
				INI_sCurrentSection = Upper$(INI_CreateSection(INI_lFileHandle, INI_sTemp))
				If (INI_sCurrentSection = INI_sUpperSection) Then INI_bSectionFound = True
			Else
				If Left(INI_sTemp, 1) = ":" Then
					WriteLine INI_lFileHandle, INI_sTemp
				Else
					; KEY=VALUE				
					Local lEqualsPos% = Instr(INI_sTemp, "=")
					
					If (lEqualsPos <> 0) Then
						If (INI_sCurrentSection = INI_sUpperSection) And (Upper$(Trim$(Left$(INI_sTemp, (lEqualsPos - 1)))) = Upper$(INI_sKey)) Then
							If (INI_sValue <> "") Then INI_CreateKey INI_lFileHandle, INI_sKey, INI_sValue
							INI_bWrittenKey = True
						Else
							WriteLine INI_lFileHandle, INI_sTemp
						EndIf
					EndIf
				EndIf
			EndIf
		EndIf
		; Move through the INI file...
		INI_lOldPos = INI_lPos + 1
		INI_lPos% = Instr(INI_sContents, Chr$(0), INI_lOldPos)
	Wend
	
	; KEY wasn;t found in the INI file - Append a New SECTION If required And create our KEY=VALUE Line
	If (INI_bWrittenKey = False) Then
		If (INI_bSectionFound = False) Then INI_CreateSection INI_lFileHandle, INI_sSection
		INI_CreateKey INI_lFileHandle, INI_sKey, INI_sValue
	EndIf
	
	CloseFile INI_lFileHandle
	
	Return True ; Success
End Function

Function INI_FileToString$(INI_sFilename$)
	Local INI_sString$ = ""
	Local INI_lFileHandle%= ReadFile(INI_sFilename)
	
	If INI_lFileHandle <> 0 Then
		While Not(Eof(INI_lFileHandle))
			INI_sString = INI_sString + ReadLine$(INI_lFileHandle) + Chr$(0)
		Wend
		CloseFile INI_lFileHandle
	EndIf
	Return INI_sString
End Function

Function INI_CreateSection$(INI_lFileHandle%, INI_sNewSection$)
	If FilePos(INI_lFileHandle) <> 0 Then WriteLine INI_lFileHandle, "" ; Blank Line between sections
	WriteLine INI_lFileHandle, INI_sNewSection
	Return INI_sNewSection
End Function

Function INI_CreateKey%(INI_lFileHandle%, INI_sKey$, INI_sValue$)
	WriteLine INI_lFileHandle, INI_sKey + " = " + INI_sValue
	Return True
End Function

Function StripFilename$(file$)
	Local mi$=""
	Local lastSlash%=0
	
	If Len(file)>0
		For i%=1 To Len(file)
			mi=Mid(file$,i,1)
			If mi="\" Lor mi="/" Then
				lastSlash=i
			EndIf
		Next
	EndIf
	Return Left(file,lastSlash)
End Function

Function StripPath$(file$) 
	Local name$=""
	
	If Len(file$)>0 
		For i=Len(file$) To 1 Step -1 
			mi$=Mid$(file$,i,1) 
			If mi$="\" Lor mi$="/" Then Return name$
			name$=mi$+name$ 
		Next 
	EndIf 
	Return name$ 
End Function

Function Piece$(s$,entry,char$=" ")
	While Instr(s,char+char)
		s=Replace(s,char+char,char)
	Wend
	For n=1 To entry-1
		p=Instr(s,char)
		s=Right(s,Len(s)-p)
	Next
	p=Instr(s,char)
	If p<1
		a$=s
	Else
		a=Left(s,p-1)
	EndIf
	Return a
End Function

Function KeyValue$(entity,key$,defaultvalue$="")
	properties$=EntityName(entity)
	properties$=Replace(properties$,Chr(13),"")
	key$=Lower(key)
	Repeat
		p=Instr(properties,Chr(10))
		If p Then test$=(Left(properties,p-1)) Else test=properties
		testkey$=Piece(test,1,"=")
		testkey=Trim(testkey)
		testkey=Replace(testkey,Chr(34),"")
		testkey=Lower(testkey)
		If testkey=key Then
			value$=Piece(test,2,"=")
			value$=Trim(value$)
			value$=Replace(value$,Chr(34),"")
			Return value
		EndIf
		If Not p Then Return defaultvalue$
		properties=Right(properties,Len(properties)-p)
	Forever 
End Function

Function GetNPCManipulationValue$(NPC$,bone$,section$,valuetype%=0)
	;valuetype determines what type of variable should the Output be returned
	;0 - String
	;1 - Int
	;2 - Float
	;3 - Boolean
	
	Local value$ = GetINIString("Data\NPCBones.ini",NPC$,bone$+"_"+section$)
	
	Select valuetype%
		Case 0
			Return value$
		Case 1
			Return Int(value$)
		Case 2
			Return Float(value$)
		Case 3
			If value$ = "true" Lor value$ = "1"
				Return True
			Else
				Return False
			EndIf
	End Select
End Function

Const OptionFile$ = "options.ini"

Global EnableSFXRelease% = GetINIInt(OptionFile, "audio", "sfx release")
Global EnableSFXRelease_Prev% = EnableSFXRelease%

Global LauncherEnabled% = GetINIInt(OptionFile, "launcher", "launcher enabled")

Global Fullscreen% = GetINIInt(OptionFile, "options", "fullscreen")

Global CanOpenConsole% = GetINIInt(OptionFile, "console", "enabled")

Global GraphicWidth% = GetINIInt(OptionFile, "options", "width")
Global GraphicHeight% = GetINIInt(OptionFile, "options", "height")

Global SelectedGFXMode%
Global SelectedGFXDriver% = Max(GetINIInt(OptionFile, "options", "gfx driver"), 1)

Global ShowFPS = GetINIInt(OptionFile, "options", "show FPS")

Global TotalGFXModes% = CountGfxModes3D(), GFXModes%
Dim GfxModeWidths%(TotalGFXModes), GfxModeHeights%(TotalGFXModes)

Global BorderlessWindowed% = GetINIInt(OptionFile, "options", "borderless windowed")
Global RealGraphicWidth%,RealGraphicHeight%
Global AspectRatioRatio#

Global EnableRoomLights% = GetINIInt(OptionFile, "options", "room lights enabled")

Global TextureDetails% = GetINIInt(OptionFile, "options", "texture details")
Global TextureFloat#
Select TextureDetails%
	Case 0
		TextureFloat# = 0.8
	Case 1
		TextureFloat# = 0.4
	Case 2
		TextureFloat# = 0.0
	Case 3
		TextureFloat# = -0.4
	Case 4
		TextureFloat# = -0.8
End Select
Global ConsoleOpening% = GetINIInt(OptionFile, "console", "auto opening")
Global SFXVolume# = GetINIFloat(OptionFile, "audio", "sound volume")
Global PrevSFXVolume# = SFXVolume#

Global Framelimit% = GetINIInt(OptionFile, "options", "framelimit")
Global Vsync% = GetINIInt(OptionFile, "options", "vsync")

Global Opt_AntiAlias = GetINIInt(OptionFile, "options", "antialias")

Global CurrFrameLimit# = (Framelimit%-19)/100.0
	
Global ScreenGamma# = GetINIFloat(OptionFile, "options", "screengamma")
	
Global KEY_RIGHT = GetINIInt(OptionFile, "binds", "Right key")
Global KEY_LEFT = GetINIInt(OptionFile, "binds", "Left key")
Global KEY_UP = GetINIInt(OptionFile, "binds", "Up key")
Global KEY_DOWN = GetINIInt(OptionFile, "binds", "Down key")

Global KEY_BLINK = GetINIInt(OptionFile, "binds", "Blink key")
Global KEY_SPRINT = GetINIInt(OptionFile, "binds", "Sprint key")
Global KEY_INV = GetINIInt(OptionFile, "binds", "Inventory key")
Global KEY_CROUCH = GetINIInt(OptionFile, "binds", "Crouch key")
Global KEY_SAVE = GetINIInt(OptionFile, "binds", "Save key")
Global KEY_CONSOLE = GetINIInt(OptionFile, "binds", "Console key")

Global MouseSmooth# = GetINIFloat(OptionFile,"options", "mouse smoothing", 1.0)
	
Global InvertMouse% = GetINIInt(OptionFile, "options", "invert mouse y")

Global BumpEnabled% = GetINIInt("options.ini", "options", "bump mapping enabled")
Global HUDenabled% = GetINIInt("options.ini", "options", "HUD enabled")

Global Brightness% = GetINIFloat("options.ini", "options", "brightness")
Global CameraFogNear# = GetINIFloat("options.ini", "options", "camera fog near")
Global CameraFogFar# = GetINIFloat("options.ini", "options", "camera fog far")

Global MouseSens# = GetINIFloat("options.ini", "options", "mouse sensitivity")

Global EnableVRam% = GetINIInt("options.ini", "options", "enable vram")

Global EnableUserTracks% = GetINIInt(OptionFile, "audio", "enable user tracks")
Global UserTrackMode% = GetINIInt(OptionFile, "audio", "user track setting")

Global ParticleAmount% = GetINIInt(OptionFile,"options","particle amount")

Global MusicVolume# = GetINIFloat(OptionFile, "audio", "music volume")
Global PrevMusicVolume# = MusicVolume#

Global AATextEnable% = GetINIInt(OptionFile, "options", "antialiased text")
Global AATextEnable_Prev% = AATextEnable

Global AchvMSGenabled% = GetINIInt("options.ini", "options", "achievement popup enabled")

; ~ Save options to .ini
Function SaveOptionsINI()
	PutINIValue(OptionFile, "options", "mouse sensitivity", MouseSens)
	PutINIValue(OptionFile, "options", "invert mouse y", InvertMouse)
	PutINIValue(OptionFile, "options", "bump mapping enabled", BumpEnabled)			
	PutINIValue(OptionFile, "options", "HUD enabled", HUDenabled)
	PutINIValue(OptionFile, "options", "screengamma", ScreenGamma)
	PutINIValue(OptionFile, "options", "antialias", Opt_AntiAlias)
	PutINIValue(OptionFile, "options", "vsync", Vsync)
	PutINIValue(OptionFile, "options", "show FPS", ShowFPS)
	PutINIValue(OptionFile, "options", "framelimit", Framelimit%)
	PutINIValue(OptionFile, "options", "achievement popup enabled", AchvMSGenabled%)
	PutINIValue(OptionFile, "options", "room lights enabled", EnableRoomLights%)
	PutINIValue(OptionFile, "options", "texture details", TextureDetails%)
	PutINIValue(OptionFile, "console", "enabled", CanOpenConsole%)
	PutINIValue(OptionFile, "console", "auto opening", ConsoleOpening%)
	PutINIValue(OptionFile, "options", "antialiased text", AATextEnable)
	PutINIValue(OptionFile, "options", "particle amount", ParticleAmount)
	PutINIValue(OptionFile, "options", "enable vram", EnableVRam)
	PutINIValue(OptionFile, "options", "mouse smoothing", MouseSmooth)
	
	PutINIValue(OptionFile, "audio", "music volume", MusicVolume)
	PutINIValue(OptionFile, "audio", "sound volume", PrevSFXVolume)
	PutINIValue(OptionFile, "audio", "sfx release", EnableSFXRelease)
	PutINIValue(OptionFile, "audio", "enable user tracks", EnableUserTracks%)
	PutINIValue(OptionFile, "audio", "user track setting", UserTrackMode%)
	
	PutINIValue(OptionFile, "binds", "Right key", KEY_RIGHT)
	PutINIValue(OptionFile, "binds", "Left key", KEY_LEFT)
	PutINIValue(OptionFile, "binds", "Up key", KEY_UP)
	PutINIValue(OptionFile, "binds", "Down key", KEY_DOWN)
	PutINIValue(OptionFile, "binds", "Blink key", KEY_BLINK)
	PutINIValue(OptionFile, "binds", "Sprint key", KEY_SPRINT)
	PutINIValue(OptionFile, "binds", "Inventory key", KEY_INV)
	PutINIValue(OptionFile, "binds", "Crouch key", KEY_CROUCH)
	PutINIValue(OptionFile, "binds", "Save key", KEY_SAVE)
	PutINIValue(OptionFile, "binds", "Console key", KEY_CONSOLE)
End Function

;~IDEal Editor Parameters:
;~C#Blitz3D