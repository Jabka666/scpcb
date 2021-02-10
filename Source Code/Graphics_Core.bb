Global fresize_image%, fresize_texture%, fresize_texture2%
Global fresize_cam%

Function InitFastResize()
	;Create Camera
	Local cam% = CreateCamera()
	CameraProjMode cam, 2
	CameraZoom cam, 0.1
	CameraClsMode cam, 0, 0
	CameraRange cam, 0.1, 1.5
	MoveEntity cam, 0, 0, -10000
	
	fresize_cam = cam
	
	;Create sprite
	Local spr% = CreateMesh(cam)
	Local sf% = CreateSurface(spr)
	AddVertex sf, -1, 1, 0, 0, 0
	AddVertex sf, 1, 1, 0, 1, 0
	AddVertex sf, -1, -1, 0, 0, 1
	AddVertex sf, 1, -1, 0, 1, 1
	AddTriangle sf, 0, 1, 2
	AddTriangle sf, 3, 2, 1
	EntityFX spr, 17
	ScaleEntity spr, 2048.0 / Float(RealGraphicWidth), 2048.0 / Float(RealGraphicHeight), 1
	PositionEntity spr, 0, 0, 1.0001
	EntityOrder spr, -100001
	EntityBlend spr, 1
	fresize_image = spr
	
	;Create texture
	fresize_texture = CreateTexture(2048, 2048, 1+256)
	fresize_texture2 = CreateTexture(2048, 2048, 1+256)
	TextureBlend fresize_texture2,3
	SetBuffer(TextureBuffer(fresize_texture2))
	ClsColor 0,0,0
	Cls
	SetBuffer(BackBuffer())
	EntityTexture spr, fresize_texture,0,0
	EntityTexture spr, fresize_texture2,0,1
	
	HideEntity fresize_cam
End Function

Function Graphics3DExt%(width%,height%,depth%=32,mode%=2)
	Graphics3D width,height,depth,mode
	InitFastResize()
	AntiAlias Opt_AntiAlias
End Function

Global ark_blur_image%, ark_blur_texture%, ark_sw%, ark_sh%
Global ark_blur_cam%

Function CreateBlurImage()
	;Create blur Camera
	Local cam% = CreateCamera()
	
	CameraProjMode cam,2
	CameraZoom cam,0.1
	CameraClsMode cam, 0, 0
	CameraRange cam, 0.1, 1.5
	MoveEntity cam, 0, 0, 10000
	ark_blur_cam = cam
	
	ark_sw = GraphicWidth
	ark_sh = GraphicHeight
	CameraViewport cam,0,0,ark_sw,ark_sh
	
	;Create sprite
	Local spr% = CreateMesh(cam)
	Local sf% = CreateSurface(spr)
	
	AddVertex sf, -1, 1, 0, 0, 0
	AddVertex sf, 1, 1, 0, 1, 0
	AddVertex sf, -1, -1, 0, 0, 1
	AddVertex sf, 1, -1, 0, 1, 1
	AddTriangle sf, 0, 1, 2
	AddTriangle sf, 3, 2, 1
	EntityFX spr, 17
	ScaleEntity spr, 2048.0 / Float(ark_sw), 2048.0 / Float(ark_sw), 1
	PositionEntity spr, 0, 0, 1.0001
	EntityOrder spr, -100000
	EntityBlend spr, 1
	ark_blur_image = spr
	
	;Create blur texture
	ark_blur_texture = CreateTexture(2048, 2048, 256)
	EntityTexture spr, ark_blur_texture
End Function

Function UpdateBlur(power#)
	EntityAlpha ark_blur_image, power#
	CopyRect 0, 0, GraphicWidth, GraphicHeight, 1024.0 - GraphicWidth/2, 1024.0 - GraphicHeight/2, BackBuffer(), TextureBuffer(ark_blur_texture)
End Function

Function ResizeImage2(image%,width%,height%)
	img% = CreateImage(width,height)
	
	oldWidth% = ImageWidth(image)
	oldHeight% = ImageHeight(image)
	CopyRect 0,0,oldWidth,oldHeight,1024-oldWidth/2,1024-oldHeight/2,ImageBuffer(image),TextureBuffer(fresize_texture)
	SetBuffer BackBuffer()
	ScaleRender(0,0,2048.0 / Float(RealGraphicWidth) * Float(width) / Float(oldWidth), 2048.0 / Float(RealGraphicWidth) * Float(height) / Float(oldHeight))
	;might want to replace Float(GraphicWidth) with Max(GraphicWidth,GraphicHeight) if portrait sizes cause issues
	;everyone uses landscape so it's probably a non-issue
	CopyRect RealGraphicWidth/2-width/2,RealGraphicHeight/2-height/2,width,height,0,0,BackBuffer(),ImageBuffer(img)
	
	FreeImage image
	Return img
End Function

Function UpdateScreenGamma()
	If BorderlessWindowed Then
		If (RealGraphicWidth<>GraphicWidth) Lor (RealGraphicHeight<>GraphicHeight) Then
			SetBuffer TextureBuffer(fresize_texture)
			ClsColor 0,0,0 : Cls
			CopyRect 0,0,GraphicWidth,GraphicHeight,1024-GraphicWidth/2,1024-GraphicHeight/2,BackBuffer(),TextureBuffer(fresize_texture)
			SetBuffer BackBuffer()
			ClsColor 0,0,0 : Cls
			ScaleRender(0,0,2048.0 / Float(GraphicWidth) * AspectRatioRatio, 2048.0 / Float(GraphicWidth) * AspectRatioRatio)
				;might want to replace Float(GraphicWidth) with Max(GraphicWidth,GraphicHeight) if portrait sizes cause issues
				;everyone uses landscape so it's probably a non-issue
		EndIf
	EndIf
	
		;not by any means a perfect solution
		;Not even proper gamma correction but it's a nice looking alternative that works in windowed mode
	If ScreenGamma>=1.0 Then
		CopyRect 0,0,RealGraphicWidth,RealGraphicHeight,1024-RealGraphicWidth/2,1024-RealGraphicHeight/2,BackBuffer(),TextureBuffer(fresize_texture)
		EntityBlend fresize_image,1
		ClsColor 0,0,0 : Cls
		ScaleRender(-1.0/Float(RealGraphicWidth),1.0/Float(RealGraphicWidth),2048.0 / Float(RealGraphicWidth),2048.0 / Float(RealGraphicWidth))
		EntityFX fresize_image,1+32
		EntityBlend fresize_image,3
		EntityAlpha fresize_image,ScreenGamma-1.0
		ScaleRender(-1.0/Float(RealGraphicWidth),1.0/Float(RealGraphicWidth),2048.0 / Float(RealGraphicWidth),2048.0 / Float(RealGraphicWidth))
	ElseIf ScreenGamma<1.0 Then ;todo: maybe optimize this if it's too slow, alternatively give players the option to disable gamma
		CopyRect 0,0,RealGraphicWidth,RealGraphicHeight,1024-RealGraphicWidth/2,1024-RealGraphicHeight/2,BackBuffer(),TextureBuffer(fresize_texture)
		EntityBlend fresize_image,1
		ClsColor 0,0,0 : Cls
		ScaleRender(-1.0/Float(RealGraphicWidth),1.0/Float(RealGraphicWidth),2048.0 / Float(RealGraphicWidth),2048.0 / Float(RealGraphicWidth))
		EntityFX fresize_image,1+32
		EntityBlend fresize_image,2
		EntityAlpha fresize_image,1.0
		SetBuffer TextureBuffer(fresize_texture2)
		ClsColor 255*ScreenGamma,255*ScreenGamma,255*ScreenGamma
		Cls
		SetBuffer BackBuffer()
		ScaleRender(-1.0/Float(RealGraphicWidth),1.0/Float(RealGraphicWidth),2048.0 / Float(RealGraphicWidth),2048.0 / Float(RealGraphicWidth))
		SetBuffer(TextureBuffer(fresize_texture2))
		ClsColor 0,0,0
		Cls
		SetBuffer(BackBuffer())
	EndIf
	EntityFX fresize_image,1
	EntityBlend fresize_image,1
	EntityAlpha fresize_image,1.0
End Function

Function RenderWorld2()
	CameraProjMode ark_blur_cam,0
	CameraProjMode Camera,1
	
	If WearingNightVision>0 And WearingNightVision<3 Then
		AmbientLight Min(Brightness*2,255), Min(Brightness*2,255), Min(Brightness*2,255)
	ElseIf WearingNightVision=3
		AmbientLight 255,255,255
	ElseIf PlayerRoom<>Null
		If (PlayerRoom\RoomTemplate\Name<>"173") And (PlayerRoom\RoomTemplate\Name<>"exit1") And (PlayerRoom\RoomTemplate\Name<>"gatea") Then
			AmbientLight Brightness, Brightness, Brightness
		EndIf
	EndIf
	
	IsNVGBlinking% = False
	HideEntity NVBlink
	
	CameraViewport Camera,0,0,GraphicWidth,GraphicHeight
	
	Local hasBattery% = 2
	Local power% = 0
	
	If (WearingNightVision=1) Lor (WearingNightVision=2)
		For i% = 0 To MaxItemAmount - 1
			If (Inventory[i]<>Null) Then
				If (WearingNightVision = 1 And Inventory[i]\itemtemplate\tempname = "nvgoggles") Lor (WearingNightVision = 2 And Inventory[i]\itemtemplate\tempname = "supernv") Then
					Inventory[i]\state = Inventory[i]\state - (FPSfactor * (0.02 * WearingNightVision))
					power%=Int(Inventory[i]\state)
					If Inventory[i]\state<=0.0 Then ;this nvg can't be used
						hasBattery = 0
						Msg = "The batteries in these night vision goggles died."
						BlinkTimer = -1.0
						MsgTimer = 350
						Exit
					ElseIf Inventory[i]\state<=100.0 Then
						hasBattery = 1
					EndIf
				EndIf
			EndIf
		Next
		
		If (hasBattery) Then
			RenderWorld()
		EndIf
	Else
		RenderWorld()
	EndIf
	
	CurrTrisAmount = TrisRendered()
	
	If hasBattery=0 And WearingNightVision<>3
		IsNVGBlinking% = True
		ShowEntity NVBlink%
	EndIf
	
	If BlinkTimer < - 16 Lor BlinkTimer > - 6
		If WearingNightVision=2 And hasBattery<>0 Then ;show a HUD
			NVTimer=NVTimer-FPSfactor
			
			If NVTimer<=0.0 Then
				For np.NPCs = Each NPCs
					np\NVX = EntityX(np\Collider,True)
					np\NVY = EntityY(np\Collider,True)
					np\NVZ = EntityZ(np\Collider,True)
				Next
				IsNVGBlinking% = True
				ShowEntity NVBlink%
				If NVTimer<=-10
					NVTimer = 600.0
				EndIf
			EndIf
			
			Color 255,255,255
			
			AASetFont Font3
			
			Local plusY% = 0
			If hasBattery=1 Then plusY% = 40
			
			AAText GraphicWidth/2,(20+plusY)*MenuScale,"REFRESHING DATA IN",True,False
			
			AAText GraphicWidth/2,(60+plusY)*MenuScale,Max(f2s(NVTimer/60.0,1),0.0),True,False
			AAText GraphicWidth/2,(100+plusY)*MenuScale,"SECONDS",True,False
			
			temp% = CreatePivot() : temp2% = CreatePivot()
			PositionEntity temp, EntityX(Collider), EntityY(Collider), EntityZ(Collider)
			
			Color 255,255,255
			
			For np.NPCs = Each NPCs
				If np\NVName<>"" And (Not np\HideFromNVG) Then ;don't waste your time if the string is empty
					PositionEntity temp2,np\NVX,np\NVY,np\NVZ
					dist# = EntityDistance(temp2,Collider)
					If dist<23.5 Then ;don't draw text if the NPC is too far away
						PointEntity temp, temp2
						yawvalue# = WrapAngle(EntityYaw(Camera) - EntityYaw(temp))
						xvalue# = 0.0
						If yawvalue > 90 And yawvalue <= 180 Then
							xvalue# = Sin(90)/90*yawvalue
						ElseIf yawvalue > 180 And yawvalue < 270 Then
							xvalue# = Sin(270)/yawvalue*270
						Else
							xvalue = Sin(yawvalue)
						EndIf
						pitchvalue# = WrapAngle(EntityPitch(Camera) - EntityPitch(temp))
						yvalue# = 0.0
						If pitchvalue > 90 And pitchvalue <= 180 Then
							yvalue# = Sin(90)/90*pitchvalue
						ElseIf pitchvalue > 180 And pitchvalue < 270 Then
							yvalue# = Sin(270)/pitchvalue*270
						Else
							yvalue# = Sin(pitchvalue)
						EndIf
						
						If (Not IsNVGBlinking%)
							AAText GraphicWidth / 2 + xvalue * (GraphicWidth / 2),GraphicHeight / 2 - yvalue * (GraphicHeight / 2),np\NVName,True,True
							AAText GraphicWidth / 2 + xvalue * (GraphicWidth / 2),GraphicHeight / 2 - yvalue * (GraphicHeight / 2) + 30.0 * MenuScale,f2s(dist,1)+" m",True,True
						EndIf
					EndIf
				EndIf
			Next
			
			FreeEntity (temp) : FreeEntity (temp2)
			
			Color 0,0,55
			For k=0 To 10
				Rect 45,GraphicHeight*0.5-(k*20),54,10,True
			Next
			Color 0,0,255
			For l=0 To Floor((power%+50)*0.01)
				Rect 45,GraphicHeight*0.5-(l*20),54,10,True
			Next
			DrawImage NVGImages,40,GraphicHeight*0.5+30,1
			
			Color 255,255,255
		ElseIf WearingNightVision=1 And hasBattery<>0
			Color 0,55,0
			For k=0 To 10
				Rect 45,GraphicHeight*0.5-(k*20),54,10,True
			Next
			Color 0,255,0
			For l=0 To Floor((power%+50)*0.01)
				Rect 45,GraphicHeight*0.5-(l*20),54,10,True
			Next
			DrawImage NVGImages,40,GraphicHeight*0.5+30,0
		EndIf
	EndIf
	
	;render sprites
	CameraProjMode ark_blur_cam,2
	CameraProjMode Camera,0
	RenderWorld()
	CameraProjMode ark_blur_cam,0
	
	If BlinkTimer < - 16 Lor BlinkTimer > - 6
		If (WearingNightVision=1 Lor WearingNightVision=2) And (hasBattery=1) And ((MilliSecs2() Mod 800) < 400) Then
			Color 255,0,0
			AASetFont Font3
			
			AAText GraphicWidth/2,20*MenuScale,"WARNING: LOW BATTERY",True,False
			Color 255,255,255
		EndIf
	EndIf
End Function

Function ScaleRender(x#,y#,hscale#=1.0,vscale#=1.0)
	If Camera<>0 Then HideEntity Camera
	WireFrame 0
	ShowEntity fresize_image
	ScaleEntity fresize_image,hscale,vscale,1.0
	PositionEntity fresize_image, x, y, 1.0001
	ShowEntity fresize_cam
	RenderWorld()
	HideEntity fresize_cam
	HideEntity fresize_image
	WireFrame WireframeState
	If Camera<>0 Then ShowEntity Camera
End Function

Function PlayStartupVideos()
	HidePointer()
	
	Local ScaledGraphicHeight%
	Local Ratio# = Float(RealGraphicWidth)/Float(RealGraphicHeight)
	
	If Ratio>1.76 And Ratio<1.78
		ScaledGraphicHeight = RealGraphicHeight
		DebugLog "Not Scaled"
	Else
		ScaledGraphicHeight% = Float(RealGraphicWidth)/(16.0/9.0)
		DebugLog "Scaled: "+ScaledGraphicHeight
	EndIf
	
	Local i, moviefile$
	
	For i = 0 To 1
		Select i
			Case 0
				moviefile$ = "GFX\menu\startup_Undertow"
			Case 1
				moviefile$ = "GFX\menu\startup_TSS"
		End Select
		
		Local SplashScreenVideo = BlitzMovie_OpenD3D(moviefile$+".avi", SystemProperty("Direct3DDevice7"), SystemProperty("DirectDraw7"))
		
		If SplashScreenVideo = 0 Then
			PutINIValue(OptionFile, "options", "play startup video", "false")
			Return
		EndIf
		SplashScreenVideo = BlitzMovie_Play()
		
		Local SplashScreenAudio = StreamSound_Strict(moviefile$+".ogg",SFXVolume,0)
		
		Repeat
			Cls
			BlitzMovie_DrawD3D(0, (RealGraphicHeight/2-ScaledGraphicHeight/2), RealGraphicWidth, ScaledGraphicHeight)
			Flip
		Until (GetKey() Lor (Not IsStreamPlaying_Strict(SplashScreenAudio)))
		StopStream_Strict(SplashScreenAudio)
		BlitzMovie_Stop()
		BlitzMovie_Close()
		
		Cls
		Flip
	Next
	
	ShowPointer()
End Function

;~IDEal Editor Parameters:
;~C#Blitz3D