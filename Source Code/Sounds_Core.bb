Function PlaySound2%(SoundHandle%, cam%, entity%, range# = 10, volume# = 1.0)
	range# = Max(range, 1.0)
	Local soundchn% = 0
	
	If volume > 0 Then 
		Local dist# = EntityDistance(cam, entity) / range#
		
		If 1 - dist# > 0 And 1 - dist# < 1
			Local panvalue# = Sin(-DeltaYaw(cam,entity))
			
			soundchn% = PlaySound_Strict (SoundHandle)
			
			ChannelVolume(soundchn, volume# * (1 - dist#)*SFXVolume#)
			ChannelPan(soundchn, panvalue)			
		EndIf
	EndIf
	Return soundchn
End Function

Function LoopSound2%(SoundHandle%, Chn%, cam%, entity%, range# = 10, volume# = 1.0)
	range# = Max(range,1.0)
	
	If volume>0 Then
		Local dist# = EntityDistance(cam, entity) / range#
		Local panvalue# = Sin(-DeltaYaw(cam,entity))
		
		If Chn = 0 Then
			Chn% = PlaySound_Strict (SoundHandle)
		Else
			If (Not ChannelPlaying(Chn)) Then Chn% = PlaySound_Strict (SoundHandle)
		EndIf
		
		ChannelVolume(Chn, volume# * (1 - dist#)*SFXVolume#)
		ChannelPan(Chn, panvalue)
	Else
		If Chn <> 0 Then
			ChannelVolume (Chn, 0)
		EndIf 
	EndIf
	Return Chn
End Function

Function UpdateSoundOrigin(Chn%, cam%, entity%, range# = 10, volume# = 1.0, SFXFactor% = True)
	range# = Max(range,1.0)
	
	If volume>0 Then
		Local dist# = EntityDistance(cam, entity) / range#
		
		If 1 - dist# > 0 And 1 - dist# < 1 Then
			Local panvalue# = Sin(-DeltaYaw(cam,entity))
			
			ChannelVolume(Chn, volume# * (1 - dist#) * ((Not SFXFactor) + (SFXFactor * SFXVolume)))
			ChannelPan(Chn, panvalue)
		EndIf
	Else
		If Chn <> 0 Then
			ChannelVolume (Chn, 0)
		EndIf 
	EndIf
End Function

Function LoadTempSound(file$)
	If TempSounds[TempSoundIndex]<>0 Then FreeSound_Strict(TempSounds[TempSoundIndex])
	TempSound = LoadSound_Strict(file)
	TempSounds[TempSoundIndex] = TempSound
	
	TempSoundIndex=(TempSoundIndex+1) Mod 10
	
	Return TempSound
End Function

Function LoadEventSound(e.Events,file$,num%=0)
	If num=0 Then
		If e\Sound<>0 Then FreeSound_Strict e\Sound : e\Sound=0
		e\Sound=LoadSound_Strict(file)
		Return e\Sound
	ElseIf num=1 Then
		If e\Sound2<>0 Then FreeSound_Strict e\Sound2 : e\Sound2=0
		e\Sound2=LoadSound_Strict(file)
		Return e\Sound2
	EndIf
End Function

Function UpdateMusic()
	If ConsoleFlush Then
		If Not ChannelPlaying(ConsoleMusPlay) Then ConsoleMusPlay = PlaySound(ConsoleMusFlush)
	ElseIf (Not PlayCustomMusic)
		If NowPlaying <> ShouldPlay ; playing the wrong clip, fade out
			CurrMusicVolume# = Max(CurrMusicVolume - (FPSfactor / 250.0), 0)
			If CurrMusicVolume = 0
				If NowPlaying<66
					StopStream_Strict(MusicCHN)
				EndIf
				NowPlaying = ShouldPlay
				MusicCHN = 0
				CurrMusic=0
			EndIf
		Else ; playing the right clip
			CurrMusicVolume = CurrMusicVolume + (MusicVolume - CurrMusicVolume) * (0.1*FPSfactor)
		EndIf
		
		If NowPlaying < 66
			If CurrMusic = 0
				MusicCHN = StreamSound_Strict("SFX\Music\"+Music[NowPlaying]+".ogg",0.0,Mode)
				CurrMusic = 1
			EndIf
			SetStreamVolume_Strict(MusicCHN,CurrMusicVolume)
		EndIf
	Else
		If FPSfactor > 0 Lor OptionsMenu = 2 Then
			If (Not ChannelPlaying(MusicCHN)) Then MusicCHN = PlaySound_Strict(CustomMusic)
			ChannelVolume MusicCHN,1.0*MusicVolume
		EndIf
	EndIf
End Function 

Function PauseSounds()
	For e.events = Each Events
		If e\soundchn <> 0 Then
			If (Not e\soundchn_isstream)
				If ChannelPlaying(e\soundchn) Then PauseChannel(e\soundchn)
			Else
				SetStreamPaused_Strict(e\soundchn,True)
			EndIf
		EndIf
		If e\soundchn2 <> 0 Then
			If (Not e\soundchn2_isstream)
				If ChannelPlaying(e\soundchn2) Then PauseChannel(e\soundchn2)
			Else
				SetStreamPaused_Strict(e\soundchn2,True)
			EndIf
		EndIf		
	Next
	
	For n.npcs = Each NPCs
		If n\soundchn <> 0 Then
			If (Not n\soundchn_isstream)
				If ChannelPlaying(n\soundchn) Then PauseChannel(n\soundchn)
			Else
				If n\soundchn_isstream=True
					SetStreamPaused_Strict(n\soundchn,True)
				EndIf
			EndIf
		EndIf
		If n\soundchn2 <> 0 Then
			If (Not n\soundchn2_isstream)
				If ChannelPlaying(n\soundchn2) Then PauseChannel(n\soundchn2)
			Else
				If n\soundchn2_isstream=True
					SetStreamPaused_Strict(n\soundchn2,True)
				EndIf
			EndIf
		EndIf
	Next	
	
	For d.doors = Each Doors
		If d\soundchn <> 0 Then
			If ChannelPlaying(d\soundchn) Then PauseChannel(d\soundchn)
		EndIf
	Next
	
	For dem.DevilEmitters = Each DevilEmitters
		If dem\soundchn <> 0 Then
			If ChannelPlaying(dem\soundchn) Then PauseChannel(dem\soundchn)
		EndIf
	Next
	
	If AmbientSFXCHN <> 0 Then
		If ChannelPlaying(AmbientSFXCHN) Then PauseChannel(AmbientSFXCHN)
	EndIf
	
	If BreathCHN <> 0 Then
		If ChannelPlaying(BreathCHN) Then PauseChannel(BreathCHN)
	EndIf
	
	If VomitCHN <> 0 Then
		If ChannelPlaying(VomitCHN) Then PauseChannel(VomitCHN)
	EndIf
	
	If CoughCHN <> 0 Then
		If ChannelPlaying(CoughCHN) Then PauseChannel(CoughCHN)
	EndIf
	
	If IntercomStreamCHN <> 0
		SetStreamPaused_Strict(IntercomStreamCHN,True)
	EndIf
End Function

Function ResumeSounds()
	For e.events = Each Events
		If e\soundchn <> 0 Then
			If (Not e\soundchn_isstream)
				If ChannelPlaying(e\soundchn) Then ResumeChannel(e\soundchn)
			Else
				SetStreamPaused_Strict(e\soundchn,False)
			EndIf
		EndIf
		If e\soundchn2 <> 0 Then
			If (Not e\soundchn2_isstream)
				If ChannelPlaying(e\soundchn2) Then ResumeChannel(e\soundchn2)
			Else
				SetStreamPaused_Strict(e\soundchn2,False)
			EndIf
		EndIf	
	Next
	
	For n.npcs = Each NPCs
		If n\soundchn <> 0 Then
			If (Not n\soundchn_isstream)
				If ChannelPlaying(n\soundchn) Then ResumeChannel(n\soundchn)
			Else
				If n\soundchn_isstream=True
					SetStreamPaused_Strict(n\soundchn,False)
				EndIf
			EndIf
		EndIf
		If n\soundchn2 <> 0 Then
			If (Not n\soundchn2_isstream)
				If ChannelPlaying(n\soundchn2) Then ResumeChannel(n\soundchn2)
			Else
				If n\soundchn2_isstream=True
					SetStreamPaused_Strict(n\soundchn2,False)
				EndIf
			EndIf
		EndIf
	Next	
	
	For d.doors = Each Doors
		If d\soundchn <> 0 Then
			If ChannelPlaying(d\soundchn) Then ResumeChannel(d\soundchn)
		EndIf
	Next
	
	For dem.DevilEmitters = Each DevilEmitters
		If dem\soundchn <> 0 Then
			If ChannelPlaying(dem\soundchn) Then ResumeChannel(dem\soundchn)
		EndIf
	Next
	
	If AmbientSFXCHN <> 0 Then
		If ChannelPlaying(AmbientSFXCHN) Then ResumeChannel(AmbientSFXCHN)
	EndIf	
	
	If BreathCHN <> 0 Then
		If ChannelPlaying(BreathCHN) Then ResumeChannel(BreathCHN)
	EndIf
	
	If VomitCHN <> 0 Then
		If ChannelPlaying(VomitCHN) Then ResumeChannel(VomitCHN)
	EndIf
	
	If CoughCHN <> 0 Then
		If ChannelPlaying(CoughCHN) Then ResumeChannel(CoughCHN)
	EndIf
	
	If IntercomStreamCHN <> 0
		SetStreamPaused_Strict(IntercomStreamCHN,False)
	EndIf
End Function

Function KillSounds()
	Local i%,e.Events,n.NPCs,d.Doors,dem.DevilEmitters,snd.Sound
	
	For i=0 To 9
		If TempSounds[i]<>0 Then FreeSound_Strict TempSounds[i] : TempSounds[i]=0
	Next
	For e.Events = Each Events
		If e\SoundCHN <> 0 Then
			If (Not e\SoundCHN_isStream)
				If ChannelPlaying(e\SoundCHN) Then StopChannel(e\SoundCHN)
			Else
				StopStream_Strict(e\SoundCHN)
			EndIf
		EndIf
		If e\SoundCHN2 <> 0 Then
			If (Not e\SoundCHN2_isStream)
				If ChannelPlaying(e\SoundCHN2) Then StopChannel(e\SoundCHN2)
			Else
				StopStream_Strict(e\SoundCHN2)
			EndIf
		EndIf		
	Next
	For n.NPCs = Each NPCs
		If n\SoundChn <> 0 Then
			If (Not n\SoundChn_IsStream)
				If ChannelPlaying(n\SoundChn) Then StopChannel(n\SoundChn)
			Else
				StopStream_Strict(n\SoundChn)
			EndIf
		EndIf
		If n\SoundChn2 <> 0 Then
			If (Not n\SoundChn2_IsStream)
				If ChannelPlaying(n\SoundChn2) Then StopChannel(n\SoundChn2)
			Else
				StopStream_Strict(n\SoundChn2)
			EndIf
		EndIf
	Next	
	For d.Doors = Each Doors
		If d\SoundCHN <> 0 Then
			If ChannelPlaying(d\SoundCHN) Then StopChannel(d\SoundCHN)
		EndIf
	Next
	For dem.DevilEmitters = Each DevilEmitters
		If dem\SoundCHN <> 0 Then
			If ChannelPlaying(dem\SoundCHN) Then StopChannel(dem\SoundCHN)
		EndIf
	Next
	If AmbientSFXCHN <> 0 Then
		If ChannelPlaying(AmbientSFXCHN) Then StopChannel(AmbientSFXCHN)
	EndIf
	If BreathCHN <> 0 Then
		If ChannelPlaying(BreathCHN) Then StopChannel(BreathCHN)
	EndIf
	If VomitCHN <> 0 Then
		If ChannelPlaying(VomitCHN) Then StopChannel(VomitCHN)
	EndIf
	If CoughCHN <> 0 Then
		If ChannelPlaying(CoughCHN) Then StopChannel(CoughCHN)
	EndIf
	If IntercomStreamCHN <> 0
		StopStream_Strict(IntercomStreamCHN)
		IntercomStreamCHN = 0
	EndIf
	If EnableSFXRelease
		For snd.Sound = Each Sound
			If snd\internalHandle <> 0 Then
				FreeSound snd\internalHandle
				snd\internalHandle = 0
				snd\releaseTime = 0
			EndIf
		Next
	EndIf
	
	For snd.Sound = Each Sound
		For i = 0 To 31
			If snd\channels[i]<>0 Then
				StopChannel snd\channels[i]
			EndIf
		Next
	Next
	DebugLog "Terminated all sounds"
End Function

Function GetStepSound(entity%)
	Local picker%,brush%,texture%,name$
	Local mat.Materials
	
	picker = LinePick(EntityX(entity),EntityY(entity),EntityZ(entity),0,-1,0)
	If picker <> 0 Then
		If GetEntityType(picker) <> HIT_MAP Then Return 0
		brush = GetSurfaceBrush(GetSurface(picker,CountSurfaces(picker)))
		If brush <> 0 Then
			texture = GetBrushTexture(brush,3)
			If texture <> 0 Then
				name = StripPath(TextureName(texture))
				If (name <> "") Then FreeTexture(texture)
				For mat.Materials = Each Materials
					If mat\name = name Then
						If mat\StepSound > 0 Then
							FreeBrush(brush)
							Return mat\StepSound-1
						EndIf
						Exit
					EndIf
				Next                
			EndIf
			texture = GetBrushTexture(brush,2)
			If texture <> 0 Then
				name = StripPath(TextureName(texture))
				If (name <> "") Then FreeTexture(texture)
				For mat.Materials = Each Materials
					If mat\name = name Then
						If mat\StepSound > 0 Then
							FreeBrush(brush)
							Return mat\StepSound-1
						EndIf
						Exit
					EndIf
				Next                
			EndIf
			texture = GetBrushTexture(brush,1)
			If texture <> 0 Then
				name = StripPath(TextureName(texture))
				If (name <> "") Then FreeTexture(texture)
				FreeBrush(brush)
				For mat.Materials = Each Materials
					If mat\name = name Then
						If mat\StepSound > 0 Then
							Return mat\StepSound-1
						EndIf
						Exit
					EndIf
				Next                
			EndIf
		EndIf
	EndIf
	Return 0
End Function

Function PlayAnnouncement(file$) ;This function streams the announcement currently playing
	If IntercomStreamCHN <> 0 Then
		StopStream_Strict(IntercomStreamCHN)
		IntercomStreamCHN = 0
	EndIf
	
	IntercomStreamCHN = StreamSound_Strict(file$,SFXVolume,0)
End Function

Function UpdateStreamSounds()
	Local e.Events
	
	If FPSfactor > 0 Then
		If IntercomStreamCHN <> 0 Then
			SetStreamVolume_Strict(IntercomStreamCHN,SFXVolume)
		EndIf
	EndIf
	
	If (Not PlayerInReachableRoom()) Then
		If PlayerRoom\RoomTemplate\Name <> "exit1" And PlayerRoom\RoomTemplate\Name <> "gatea" Then
			If IntercomStreamCHN <> 0 Then
				StopStream_Strict(IntercomStreamCHN)
				IntercomStreamCHN = 0
			EndIf
		EndIf
	EndIf
End Function

Function ControlSoundVolume()
	Local snd.Sound,i
	
	For snd.Sound = Each Sound
		For i=0 To 31
			ChannelVolume snd\channels[i],SFXVolume#
		Next
	Next
End Function

Function UpdateDeafPlayer()
	If DeafTimer > 0
		DeafTimer = DeafTimer-FPSfactor
		SFXVolume# = 0.0
		If SFXVolume# > 0.0
			ControlSoundVolume()
		EndIf
		DebugLog DeafTimer
	Else
		DeafTimer = 0
		
		SFXVolume# = PrevSFXVolume#
		If DeafPlayer Then ControlSoundVolume()
		DeafPlayer = False
	EndIf
End Function

Function PlayMTFSound(sound%, n.NPCs)
	If n <> Null Then
		n\SoundChn = PlaySound2(sound, Camera, n\Collider, 8.0)	
	EndIf
	
	If SelectedItem <> Null Then
		If SelectedItem\state2 = 3 And SelectedItem\state > 0 Then 
			Select SelectedItem\itemtemplate\tempname 
				Case "radio","fineradio","18vradio"
					If sound<>MTFSFX[0] Lor (Not ChannelPlaying(RadioCHN[3]))
						If RadioCHN[3]<> 0 Then StopChannel RadioCHN[3]
						RadioCHN[3] = PlaySound_Strict (sound)
					EndIf
			End Select
		EndIf
	EndIf 
End Function

Global SoundEmitter%
Global TempSounds%[10]
Global TempSoundCHN%
Global TempSoundIndex% = 0

;The Music now has to be pre-defined, as the new system uses streaming instead of the usual sound loading system Blitz3D has
Global Music$[26]
Music[0] = "The Dread"
Music[1] = "HeavyContainment"
Music[2] = "EntranceZone"
Music[3] = "PD"
Music[4] = "079"
Music[5] = "GateB1"
Music[6] = "GateB2"
Music[7] = "Room3Storage"
Music[8] = "Room049"
Music[9] = "8601"
Music[10] = "106"
Music[11] = "Menu"
Music[12] = "8601Cancer"
Music[13] = "Intro"
Music[14] = "178"
Music[15] = "PDTrench"
Music[16] = "205"
Music[17] = "GateA"
Music[18] = "1499"
Music[19] = "1499Danger"
Music[20] = "049Chase"
Music[21] = "..\Ending\MenuBreath"
Music[22] = "914"
Music[23] = "Ending"
Music[24] = "Credits"
Music[25] = "SaveMeFrom"

Global CurrMusicStream, MusicCHN

MusicCHN = StreamSound_Strict("SFX\Music\"+Music[2]+".ogg",MusicVolume,Mode)

Global CurrMusicVolume# = 1.0, NowPlaying%=2, ShouldPlay%=11
Global CurrMusic% = 1

DrawLoading(10, True)

Dim OpenDoorSFX%(3,3), CloseDoorSFX%(3,3)

Global KeyCardSFX1 
Global KeyCardSFX2 
Global ButtonSFX2 
Global ScannerSFX1
Global ScannerSFX2 

Global OpenDoorFastSFX
Global CautionSFX% 

Global NuclearSirenSFX%

Global CameraSFX  

Global StoneDragSFX% 

Global GunshotSFX% 
Global Gunshot2SFX% 
Global Gunshot3SFX% 
Global BullethitSFX% 

Global TeslaIdleSFX 
Global TeslaActivateSFX 
Global TeslaPowerUpSFX 

Global MagnetUpSFX%, MagnetDownSFX
Global FemurBreakerSFX%
Global EndBreathCHN%
Global EndBreathSFX%

Global DecaySFX%[5]

Global BurstSFX 

Global HissSFX%

DrawLoading(20, True)

Global RustleSFX%[3]

Global Use914SFX%
Global Death914SFX% 

Global DripSFX%[4]

Global LeverSFX%, LightSFX% 
Global ButtGhostSFX% 

Dim RadioSFX(5,10) 

Global RadioSquelch 
Global RadioStatic 
Global RadioBuzz 

Global ElevatorBeepSFX, ElevatorMoveSFX  

Global PickSFX%[4]

Global AmbientSFXCHN%, CurrAmbientSFX%
Global AmbientSFXAmount[6]
;0 = light containment, 1 = heavy containment, 2 = entrance
AmbientSFXAmount[0]=8 : AmbientSFXAmount[1]=11 : AmbientSFXAmount[2]=12
;3 = general, 4 = pre-breach
AmbientSFXAmount[3]=15 : AmbientSFXAmount[4]=5
;5 = forest
AmbientSFXAmount[5]=10

Dim AmbientSFX%(6, 15)

Global OldManSFX%[9]

Global Scp173SFX%[3]

Global HorrorSFX%[16]

DrawLoading(25, True)

Global IntroSFX%[20]

Global AlarmSFX%[5]

Global CommotionState%[25]

Global HeartBeatSFX 

Global VomitSFX%

Dim BreathSFX(2,5)
Global BreathCHN%

Global NeckSnapSFX[3]

Global DamageSFX%[9]

Global MTFSFX%[2]

Global CoughSFX%[3]
Global CoughCHN%, VomitCHN%

Global MachineSFX% 
Global ApacheSFX
Global CurrStepSFX
Dim StepSFX%(5, 2, 8) ;(normal/metal, walk/run, id)

Global Step2SFX[6]

Global RadioCHN%[7]

Global IntercomStreamCHN%

DrawLoading(30, True)

Global PlayCustomMusic% = False, CustomMusic% = 0

Global UserTrackCheck% = 0, UserTrackCheck2% = 0
Global UserTrackMusicAmount% = 0, CurrUserTrack%, UserTrackFlag% = False
Global UserTrackName$[256]

Function LoadAllSounds()
	For i = 0 To 2
		OpenDoorSFX(0,i) = LoadSound_Strict("SFX\Door\DoorOpen" + (i + 1) + ".ogg")
		CloseDoorSFX(0,i) = LoadSound_Strict("SFX\Door\DoorClose" + (i + 1) + ".ogg")
		OpenDoorSFX(2,i) = LoadSound_Strict("SFX\Door\Door2Open" + (i + 1) + ".ogg")
		CloseDoorSFX(2,i) = LoadSound_Strict("SFX\Door\Door2Close" + (i + 1) + ".ogg")
		OpenDoorSFX(3,i) = LoadSound_Strict("SFX\Door\ElevatorOpen" + (i + 1) + ".ogg")
		CloseDoorSFX(3,i) = LoadSound_Strict("SFX\Door\ElevatorClose" + (i + 1) + ".ogg")
	Next
	For i = 0 To 1
		OpenDoorSFX(1,i) = LoadSound_Strict("SFX\Door\BigDoorOpen" + (i + 1) + ".ogg")
		CloseDoorSFX(1,i) = LoadSound_Strict("SFX\Door\BigDoorClose" + (i + 1) + ".ogg")
	Next
	
	KeyCardSFX1 = LoadSound_Strict("SFX\Interact\KeyCardUse1.ogg")
	KeyCardSFX2 = LoadSound_Strict("SFX\Interact\KeyCardUse2.ogg")
	ButtonSFX2 = LoadSound_Strict("SFX\Interact\Button2.ogg")
	ScannerSFX1 = LoadSound_Strict("SFX\Interact\ScannerUse1.ogg")
	ScannerSFX2 = LoadSound_Strict("SFX\Interact\ScannerUse2.ogg")
	
	OpenDoorFastSFX=LoadSound_Strict("SFX\Door\DoorOpenFast.ogg")
	CautionSFX% = LoadSound_Strict("SFX\Room\LockroomSiren.ogg")
	
	CameraSFX = LoadSound_Strict("SFX\General\Camera.ogg") 
	
	StoneDragSFX% = LoadSound_Strict("SFX\SCP\173\StoneDrag.ogg")
	
	GunshotSFX% = LoadSound_Strict("SFX\General\Gunshot.ogg")
	Gunshot2SFX% = LoadSound_Strict("SFX\General\Gunshot2.ogg")
	Gunshot3SFX% = LoadSound_Strict("SFX\General\BulletMiss.ogg")
	BullethitSFX% = LoadSound_Strict("SFX\General\BulletHit.ogg")
	
	TeslaIdleSFX = LoadSound_Strict("SFX\Room\Tesla\Idle.ogg")
	TeslaActivateSFX = LoadSound_Strict("SFX\Room\Tesla\WindUp.ogg")
	TeslaPowerUpSFX = LoadSound_Strict("SFX\Room\Tesla\PowerUp.ogg")
	
	MagnetUpSFX% = LoadSound_Strict("SFX\Room\106Chamber\MagnetUp.ogg") 
	MagnetDownSFX = LoadSound_Strict("SFX\Room\106Chamber\MagnetDown.ogg")
	
	For i = 0 To 3
		DecaySFX[i] = LoadSound_Strict("SFX\SCP\106\Decay" + i + ".ogg")
	Next
	
	BurstSFX = LoadSound_Strict("SFX\Room\TunnelBurst.ogg")
	
	HissSFX = LoadSound_Strict("SFX\General\Hiss.ogg")
	
	For i = 0 To 2
		RustleSFX[i] = LoadSound_Strict("SFX\SCP\372\Rustle" + i + ".ogg")
	Next
	
	Death914SFX% = LoadSound_Strict("SFX\SCP\914\PlayerDeath.ogg") 
	Use914SFX% = LoadSound_Strict("SFX\SCP\914\PlayerUse.ogg")
	
	For i = 0 To 3
		DripSFX[i] = LoadSound_Strict("SFX\Character\D9341\BloodDrip" + i + ".ogg")
	Next
	
	LeverSFX% = LoadSound_Strict("SFX\Interact\LeverFlip.ogg") 
	LightSFX% = LoadSound_Strict("SFX\General\LightSwitch.ogg")
	
	ButtGhostSFX% = LoadSound_Strict("SFX\SCP\Joke\789J.ogg")
	
	RadioSFX(1,0) = LoadSound_Strict("SFX\Radio\RadioAlarm.ogg")
	RadioSFX(1,1) = LoadSound_Strict("SFX\Radio\RadioAlarm2.ogg")
	For i = 0 To 8
		RadioSFX(2,i) = LoadSound_Strict("SFX\Radio\scpradio"+i+".ogg")
	Next
	RadioSquelch = LoadSound_Strict("SFX\Radio\squelch.ogg")
	RadioStatic = LoadSound_Strict("SFX\Radio\static.ogg")
	RadioBuzz = LoadSound_Strict("SFX\Radio\buzz.ogg")
	
	ElevatorBeepSFX = LoadSound_Strict("SFX\General\Elevator\Beep.ogg") 
	ElevatorMoveSFX = LoadSound_Strict("SFX\General\Elevator\Moving.ogg") 
	
	For i = 0 To 3
		PickSFX[i] = LoadSound_Strict("SFX\Interact\PickItem" + i + ".ogg")
	Next
	
	;0 = light containment, 1 = heavy containment, 2 = entrance
	AmbientSFXAmount[0]=11 : AmbientSFXAmount[1]=11 : AmbientSFXAmount[2]=12
	;3 = general, 4 = pre-breach
	AmbientSFXAmount[3]=15 : AmbientSFXAmount[4]=5
	;5 = forest
	AmbientSFXAmount[5]=10
	
	For i = 0 To 2
		OldManSFX[i] = LoadSound_Strict("SFX\SCP\106\Corrosion" + (i + 1) + ".ogg")
	Next
	OldManSFX[3] = LoadSound_Strict("SFX\SCP\106\Laugh.ogg")
	OldManSFX[4] = LoadSound_Strict("SFX\SCP\106\Breathing.ogg")
	OldManSFX[5] = LoadSound_Strict("SFX\Room\PocketDimension\Enter.ogg")
	For i = 0 To 2
		OldManSFX[6+i] = LoadSound_Strict("SFX\SCP\106\WallDecay"+(i+1)+".ogg")
	Next
	
	For i = 0 To 2
		Scp173SFX[i] = LoadSound_Strict("SFX\SCP\173\Rattle" + (i + 1) + ".ogg")
	Next
	
	For i = 0 To 11
		HorrorSFX[i] = LoadSound_Strict("SFX\Horror\Horror" + i + ".ogg")
	Next
	For i = 14 To 15
		HorrorSFX[i] = LoadSound_Strict("SFX\Horror\Horror" + i + ".ogg")
	Next
	
	For i = 7 To 9
		IntroSFX[i] = LoadSound_Strict("SFX\Room\Intro\Bang" + (i - 6) + ".ogg")
	Next
	For i = 10 To 12
		IntroSFX[i] = LoadSound_Strict("SFX\Room\Intro\Light" + (i - 9) + ".ogg")
	Next
	IntroSFX[15] = LoadSound_Strict("SFX\Room\Intro\173Vent.ogg")
	
	AlarmSFX[0] = LoadSound_Strict("SFX\Alarm\Alarm.ogg")
	AlarmSFX[2] = LoadSound_Strict("SFX\Alarm\Alarm3.ogg")
	
	;room_gw alarms
	AlarmSFX[3] = LoadSound_Strict("SFX\Alarm\Alarm4.ogg")
	AlarmSFX[4] = LoadSound_Strict("SFX\Alarm\Alarm5.ogg")
	
	HeartBeatSFX = LoadSound_Strict("SFX\Character\D9341\Heartbeat.ogg")
	
	For i = 0 To 4
		BreathSFX(0,i)=LoadSound_Strict("SFX\Character\D9341\breath"+i+".ogg")
		BreathSFX(1,i)=LoadSound_Strict("SFX\Character\D9341\breath"+i+"gas.ogg")
	Next
	
	For i = 0 To 2
		NeckSnapSFX[i] = LoadSound_Strict("SFX\SCP\173\NeckSnap"+(i+1)+".ogg")
	Next
	
	For i = 0 To 8
		DamageSFX[i] = LoadSound_Strict("SFX\Character\D9341\Damage"+(i+1)+".ogg")
	Next
	
	For i = 0 To 2
		CoughSFX[i] = LoadSound_Strict("SFX\Character\D9341\Cough" + (i + 1) + ".ogg")
	Next
	
	MachineSFX% = LoadSound_Strict("SFX\SCP\914\Refining.ogg")
	
	ApacheSFX = LoadSound_Strict("SFX\Character\Apache\Propeller.ogg")
	
	For i = 0 To 7
		StepSFX(0, 0, i) = LoadSound_Strict("SFX\Step\Step" + (i + 1) + ".ogg")
		StepSFX(1, 0, i) = LoadSound_Strict("SFX\Step\StepMetal" + (i + 1) + ".ogg")
		StepSFX(0, 1, i)= LoadSound_Strict("SFX\Step\Run" + (i + 1) + ".ogg")
		StepSFX(1, 1, i) = LoadSound_Strict("SFX\Step\RunMetal" + (i + 1) + ".ogg")
		If i < 3
			StepSFX(2, 0, i) = LoadSound_Strict("SFX\Character\MTF\Step" + (i + 1) + ".ogg")
			StepSFX(3, 0, i) = LoadSound_Strict("SFX\SCP\049\Step"+ (i + 1) + ".ogg")
		EndIf
		If i < 4
			StepSFX(4, 0, i) = LoadSound_Strict("SFX\Step\SCP\StepSCP" + (i + 1) + ".ogg");new one 1.3.9
		EndIf
	Next
	
	For i = 0 To 2
		Step2SFX[i] = LoadSound_Strict("SFX\Step\StepPD" + (i + 1) + ".ogg")
		Step2SFX[i+3] = LoadSound_Strict("SFX\Step\StepForest" + (i + 1) + ".ogg")
	Next 
End Function

;~IDEal Editor Parameters:
;~C#Blitz3D