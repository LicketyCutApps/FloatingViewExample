# FloatingViewExample
 Demonstration of two different types of [Floating Views](app/src/main/java/com/licketycut/floatingviewexample/FloatingView.java).

## FloatingView
 An abstract class created to facilitate custom views which are attached to the Window Manager and float on top of the display.

 Many different layouts could be designed to take advantage of the base class. For this example I have created these two.
 
* ## FloatingButtonView
   An extension of the FloatingView class, which consists of a button and popup menu which can be clicked, dragged and long pressed for menu.

* ## FloatingInfoView
   An extension of the FloatingView class, which consists of an information rectangle with title and text which can be clicked and swiped away.
 
## FloatingViewService
 A service designed to manage floating views and communicate between them and an activity.

## FloatingViewTouchListener
 A custom touch listener allowing the user to interact with, move and perform clicks and gestures on floating views.


---

[<img src="https://j.gifs.com/zvXgo5.gif" width="200" height="355">](https://youtu.be/pR_YjVAI_mU)

#### Author
[Adam Claflin - LicketyCutApps](https://github.com/LicketyCutApps)

#### License
 Copyright 2019 Adam Claflin [adam.r.claflin@gmail.com].

 Licensed under the Attribution-NonCommercial 4.0 International (CC BY-NC 4.0);
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

	https://creativecommons.org/licenses/by-nc/4.0/

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 ##### Compatibility: Minimum Android SDK: API level 19
