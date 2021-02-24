Function GenerateSeedNumber(seed$)
	Local temp% = 0
	Local shift% = 0
	
	For i = 1 To Len(seed)
		temp = temp Xor (Asc(Mid(seed,i,1)) Shl shift)
		shift=(shift+1) Mod 24
	Next
	Return temp
End Function

Function CurveValue#(number#, old#, smooth#)
	If FPSfactor = 0 Then Return old
	
	If number < old Then
		Return Max(old + (number - old) * (1.0 / smooth * FPSfactor), number)
	Else
		Return Min(old + (number - old) * (1.0 / smooth * FPSfactor), number)
	EndIf
End Function

Function CurveAngle#(val#, old#, smooth#)
	If FPSfactor = 0 Then Return old
	
	Local diff# = WrapAngle(val) - WrapAngle(old)
	
	If diff > 180 Then diff = diff - 360
	If diff < - 180 Then diff = diff + 360
	Return WrapAngle(old + diff * (1.0 / smooth * FPSfactor))
End Function

Function WrapAngle#(angle#)
	If angle = Infinity Then Return(0.0)
	If angle < 0.0 Then
		Return(360.0 + (angle Mod 360.0))
	Else
		Return(angle Mod 360.0)
	EndIf
End Function

Function point_direction#(x1#,z1#,x2#,z2#)
	Local dx#, dz#
	
	dx = x1 - x2
	dz = z1 - z2
	Return ATan2(dz,dx)
End Function

Function angleDist#(a0#,a1#)
	Local b# = a0-a1
	Local bb#
	
	If b<-180.0 Then
		bb = b+360.0
	ElseIf b>180.0 Then
		bb = b-360.0
	Else
		bb = b
	EndIf
	Return bb
End Function

Function f2s$(n#, count%)
	Return Left(n, Len(Int(n))+count+1)
End Function

Function ChangeAngleValueForCorrectBoneAssigning(value#)
	Local numb#
	
	If value# <= 180.0
		numb# = value#
	Else
		numb# = -360+value#
	EndIf
	
	Return numb#
End Function

Function move_forward%(dir%,pathx%,pathy%,retval%=0)
	;move 1 unit along the grid in the designated direction
	If dir = 1 Then
		If retval=0 Then
			Return pathx
		Else
			Return pathy+1
		EndIf
	EndIf
	If retval=0 Then
		Return pathx-1+dir
	Else
		Return pathy
	EndIf
End Function

Function MilliSecs2()
	Local retVal% = MilliSecs()
	
	If retVal < 0 Then retVal = retVal + 2147483648
	Return retVal
End Function

Function chance%(value%)
	;perform a chance given a probability
	Return (Rand(0,100)<=value)
End Function

Function turn_if_deviating%(max_deviation_distance_%,pathx%,center_%,dir%,retval%=0)
	;check if deviating and return the answer. if deviating, turn around
	Local current_deviation% = center_ - pathx
	Local deviated% = False
	
	If (dir = 0 And current_deviation >= max_deviation_distance_) Lor (dir = 2 And current_deviation <= -max_deviation_distance_) Then
		dir = (dir + 2) Mod 4
		deviated = True
	EndIf
	If retval=0 Then Return dir Else Return deviated
End Function

Function ScaledMouseX%()
	Return Float(MouseX()-(RealGraphicWidth*0.5*(1.0-AspectRatioRatio)))*Float(GraphicWidth)/Float(RealGraphicWidth*AspectRatioRatio)
End Function

Function ScaledMouseY%()
	Return Float(MouseY())*Float(GraphicHeight)/Float(RealGraphicHeight)
End Function

Function MouseOn%(x%, y%, width%, height%)
	If ScaledMouseX() > x And ScaledMouseX() < x + width Then
		If ScaledMouseY() > y And ScaledMouseY() < y + height Then
			Return True
		EndIf
	EndIf
	Return False
End Function

Function Find860Angle(n.NPCs, fr.Forest)
	TFormPoint(EntityX(Collider),EntityY(Collider),EntityZ(Collider),0,fr\Forest_Pivot)
	
	Local playerx = Floor((TFormedX()+6.0)/12.0)
	Local playerz = Floor((TFormedZ()+6.0)/12.0)
	
	TFormPoint(EntityX(n\Collider),EntityY(n\Collider),EntityZ(n\Collider),0,fr\Forest_Pivot)
	
	Local x# = (TFormedX()+6.0)/12.0
	Local z# = (TFormedZ()+6.0)/12.0
	Local xt = Floor(x), zt = Floor(z)
	Local x2,z2
	
	If xt<>playerx Lor zt<>playerz Then ;the monster is not on the same tile as the player
		For x2 = Max(xt-1,0) To Min(xt+1,gridsize-1)
			For z2 = Max(zt-1,0) To Min(zt+1,gridsize-1)
				If fr\grid[(z2*gridsize)+x2]>0 And (x2<>xt Lor z2<>zt) And (x2=xt Lor z2=zt) Then
					;tile (x2,z2) is closer to the player than the monsters current tile
					If (Abs(playerx-x2)+Abs(playerz-z2))<(Abs(playerx-xt)+Abs(playerz-zt)) Then
						;calculate the position of the tile in world coordinates
						TFormPoint(x2*12.0,0,z2*12.0,fr\Forest_Pivot,0)
						
						Return point_direction(EntityX(n\Collider),EntityZ(n\Collider),TFormedX(),TFormedZ())+180
					EndIf
					
				EndIf
			Next
		Next
	Else
		Return point_direction(EntityX(n\Collider),EntityZ(n\Collider),EntityX(Collider),EntityZ(Collider))+180
	EndIf		
End Function

Global Mesh_MinX#, Mesh_MinY#, Mesh_MinZ#
Global Mesh_MaxX#, Mesh_MaxY#, Mesh_MaxZ#
Global Mesh_MagX#, Mesh_MagY#, Mesh_MagZ#

; Create a collision box For a mesh entity taking into account entity scale
; (will not work in non-uniform scaled space)
Function MakeCollBox(mesh%)
	Local sx# = EntityScaleX(mesh, 1)
	Local sy# = Max(EntityScaleY(mesh, 1), 0.001)
	Local sz# = EntityScaleZ(mesh, 1)
	
	GetMeshExtents(mesh)
	EntityBox mesh, Mesh_MinX * sx, Mesh_MinY * sy, Mesh_MinZ * sz, Mesh_MagX * sx, Mesh_MagY * sy, Mesh_MagZ * sz
End Function

; Find mesh extents
Function GetMeshExtents(Mesh%)
	Local s%, surf%, surfs%, v%, verts%, x#, y#, z#
	Local minx# = Infinity
	Local miny# = Infinity
	Local minz# = Infinity
	Local maxx# = -Infinity
	Local maxy# = -Infinity
	Local maxz# = -Infinity
	
	surfs = CountSurfaces(Mesh)
	
	For s = 1 To surfs
		surf = GetSurface(Mesh, s)
		verts = CountVertices(surf)
		For v = 0 To verts - 1
			x = VertexX(surf, v)
			y = VertexY(surf, v)
			z = VertexZ(surf, v)
			
			If (x < minx) Then minx = x
			If (x > maxx) Then maxx = x
			If (y < miny) Then miny = y
			If (y > maxy) Then maxy = y
			If (z < minz) Then minz = z
			If (z > maxz) Then maxz = z
		Next
	Next
	
	Mesh_MinX = minx
	Mesh_MinY = miny
	Mesh_MinZ = minz
	Mesh_MaxX = maxx
	Mesh_MaxY = maxy
	Mesh_MaxZ = maxz
	Mesh_MagX = maxx-minx
	Mesh_MagY = maxy-miny
	Mesh_MagZ = maxz-minz
End Function

Function CreateLine(x1#,y1#,z1#, x2#,y2#,z2#, mesh=0)
	If mesh = 0 Then 
		mesh=CreateMesh()
		EntityFX(mesh,16)
		surf=CreateSurface(mesh)	
		verts = 0	
		
		AddVertex surf,x1#,y1#,z1#,0,0
	Else
		surf = GetSurface(mesh,1)
		verts = CountVertices(surf)-1
	EndIf
	
	AddVertex surf,(x1#+x2#)/2,(y1#+y2#)/2,(z1#+z2#)/2,0,0 
	; you could skip creating the above vertex and change the line below to
	; AddTriangle surf,verts,verts+1,verts+0
	; so your line mesh would use less vertices, the drawback is that some videocards (like the matrox g400)
	; aren't able to create a triangle with 2 vertices. so, it's your call :)
	AddVertex surf,x2#,y2#,z2#,1,0
	
	AddTriangle surf,verts,verts+2,verts+1
	
	Return mesh
End Function

Const ZONEAMOUNT% = 3

Function GetZone(y%)
	Return Min(Floor((Float(MapSize-y)/MapSize*ZONEAMOUNT)),ZONEAMOUNT-1)
End Function

Function CalculateRoomTemplateExtents(r.RoomTemplates)
	If r\DisableOverlapCheck Then Return
	
	GetMeshExtents(GetChild(r\obj,2))
	r\MinX = Mesh_MinX
	r\MinY = Mesh_MinY
	r\MinZ = Mesh_MinZ
	r\MaxX = Mesh_MaxX
	r\MaxY = Mesh_MaxY
	r\MaxZ = Mesh_MaxZ
	
	DebugLog("roomtemplateextents: "+r\MinX+", "+r\MinY	+", "+r\MinZ	+", "+r\MaxX	+", "+r\MaxY+", "+r\MaxZ)
End Function

; ~ Shrink the extents slightly - we don't care if the overlap is smaller than the thickness of the walls
Const ShrinkAmount# = 0.05

Function CalculateRoomExtents(r.Rooms)
	If r\RoomTemplate\DisableOverlapCheck Then Return
	
	;convert from the rooms local space to world space
	TFormVector(r\RoomTemplate\MinX, r\RoomTemplate\MinY, r\RoomTemplate\MinZ, r\obj, 0)
	r\MinX = TFormedX() + ShrinkAmount + r\x
	r\MinY = TFormedY() + ShrinkAmount
	r\MinZ = TFormedZ() + ShrinkAmount + r\z
	
	;convert from the rooms local space to world space
	TFormVector(r\RoomTemplate\MaxX, r\RoomTemplate\MaxY, r\RoomTemplate\MaxZ, r\obj, 0)
	r\MaxX = TFormedX() - ShrinkAmount + r\x
	r\MaxY = TFormedY() - ShrinkAmount
	r\MaxZ = TFormedZ() - ShrinkAmount + r\z
	
	If (r\MinX > r\MaxX) Then
		Local tempX# = r\MaxX
		
		r\MaxX = r\MinX
		r\MinX = tempX
	EndIf
	If (r\MinZ > r\MaxZ) Then
		Local tempZ# = r\MaxZ
		
		r\MaxZ = r\MinZ
		r\MinZ = tempZ
	EndIf
	
	DebugLog("roomextents: "+r\MinX+", "+r\MinY	+", "+r\MinZ	+", "+r\MaxX	+", "+r\MaxY+", "+r\MaxZ)
End Function

Function CheckRoomOverlap(r1.Rooms, r2.Rooms)
	If (r1\MaxX	<= r2\MinX Lor r1\MaxY <= r2\MinY Lor r1\MaxZ <= r2\MinZ) Then Return False
	If (r1\MinX	>= r2\MaxX Lor r1\MinY >= r2\MaxY Lor r1\MinZ >= r2\MaxZ) Then Return False
	
	Return True
End Function

;~IDEal Editor Parameters:
;~C#Blitz3D