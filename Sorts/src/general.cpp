/*
    This file is part of Sorts, an interface between Soar and ORTS.
    (c) 2006 James Irizarry, Sam Wintermute, and Joseph Xu

    Sorts is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    Sorts is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Sorts; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA    
*/
#include "general.h"
#include <cmath>
#include <iostream>
#include <sstream>
#include <sys/time.h>

#include "GameConst.H"
#include "GameObj.H"
#include "ScriptObj.H"

#include "Circle.h"
#include "Vec2d.h"
#include "Rectangle.h"

using namespace std;

string catStrInt(const char* str, int x) {
    ostringstream sstr;
    sstr << str << x;
    return sstr.str();
}

string int2str(int x) {
    ostringstream sstr;
    sstr << x;
    return sstr.str();
}

const char* getCommandParameter(sml::Identifier* c, const char *name) {
    const char *val = c->GetParameterValue(name);
    if (strlen(val) == 0) {
        cout << "Error: Parameter " << name << " does not exist." << endl;
        fflush(stdout);
        exit(1);
    }
    return val;
}

double squaredDistance(int x1, int y1, int x2, int y2) {
  return ((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1));
}

Vec2d getHeadingVector(int gameHeading) {
  double adeg = GameConst::angle_from_dir(gameHeading, GameConst::HEADING_N);
  double arad = adeg * PI / 180.0;
  return Vec2d(-1 * cos(arad), sin(arad));
}

Vec2d getDamageVector(int damageDir) {
  double adeg = GameConst::angle_from_dir(damageDir, GameConst::DAMAGE_N);
  double arad = adeg * PI / 180.0;
  return Vec2d(-1 * cos(arad), sin(arad));
}

double coordDistanceSq(coordinate c1, coordinate c2) {
  return ((c2.x-c1.x)*(c2.x-c1.x)+(c2.y-c1.y)*(c2.y-c1.y));
}

double coordDistance(coordinate c1, coordinate c2) {
  return sqrt((double)(((c2.x-c1.x)*(c2.x-c1.x)+(c2.y-c1.y)*(c2.y-c1.y))));
}

bool operator ==(const coordinate& c1, const coordinate& c2) {
  return ((c1.x == c2.x) && (c1.y == c2.y));
}

ostream& operator << (ostream& os, const coordinate& c) {
   return os<< c.x << "," << c.y;
}

/******************************************************
 * WEAPON RELATED FUNCTIONS                           *
 ******************************************************/

double weaponDamageRate(GameObj* gob) {
  ScriptObj* weapon = gob->component("weapon");
  if (weapon == NULL) { 
    return 0; 
    // this might not actually be right, since spells are threats too
  }

  double minDmg = weapon->get_int("min_damage");
  double maxDmg = weapon->get_int("max_damage");
  double cooldown = weapon->get_int("cooldown");

  return (minDmg + maxDmg) / (2 * cooldown); // hp / time
}

bool canHit(GameObj *atk, GameObj *tgt) {
  ScriptObj* weapon = atk->component("weapon");
  if (weapon == NULL) {
    return false;
  }
  double wr; 
  
  if (*tgt->sod.zcat == GameObj::ON_LAND) {
    wr = weapon->get_int("max_ground_range") + *atk->sod.radius;
  }
  else {
    wr = weapon->get_int("max_air_range") + *atk->sod.radius;
  }
  if (*tgt->sod.shape == SHAPE_RECTANGLE) {
    Circle c(*atk->sod.x, *atk->sod.y, wr);
    Rectangle r(*tgt->sod.x1, *tgt->sod.x2, *tgt->sod.y1, *tgt->sod.y2);
    return r.intersects(c);
  }
  else {
    double d = squaredDistance(*atk->sod.x, *atk->sod.y, *tgt->sod.x, *tgt->sod.y);
    wr += *tgt->sod.radius;
    return wr * wr >= d;
  }
}

bool canHit(GameObj *gob, const Circle& c, bool isGround) {
  ScriptObj* weapon = gob->component("weapon");
  if (weapon == NULL) {
    return false;
  }
  double d = squaredDistance(*gob->sod.x, *gob->sod.y, (int) c.x, (int) c.y);
  double r;
  if (isGround) {
    r = weapon->get_int("max_ground_range") + *gob->sod.radius + c.r;
  }
  else {
    r = weapon->get_int("max_air_range") + *gob->sod.radius + c.r;
  }
  return r * r >= d;
}

bool canHit(GameObj* atk, const Vec2d& loc, GameObj *tgt) {
  ScriptObj* weapon = atk->component("weapon");
  if (weapon == NULL) {
    return false;
  }

  double wr;
  if (*tgt->sod.zcat == GameObj::ON_LAND) {
    wr = weapon->get_int("max_ground_range") + *atk->sod.radius;
  }
  else {
    wr = weapon->get_int("max_air_range") + *atk->sod.radius;
  }
  if (*tgt->sod.shape == SHAPE_RECTANGLE) {
    Circle c(loc(0), loc(1), wr);
    Rectangle r(*tgt->sod.x1, *tgt->sod.x2, *tgt->sod.y1, *tgt->sod.y2);
    return r.intersects(c);
  }
  else {
    double d = squaredDistance((int) loc(0), (int) loc(1), *tgt->sod.x, *tgt->sod.y);
    wr += *tgt->sod.radius;
    return wr * wr >= d;
  }
}

bool canHit(const Circle& c1, const Circle& c2, double range) {
  double d = squaredDistance((int) c1.x, (int) c1.y, (int) c2.x, (int) c2.y);
  double r = range + c1.r + c2.r;
  return r * r >= d;
}

/* Returns the points on a circle that are a chordLen apart.
 * Used by AttackManager and DefendFSM.
 */
void positionsOnCircle
( const Vec2d& center, 
  const Vec2d& firstPos, 
  double chordLen,
  list<Vec2d>& positions) 
{
  Vec2d radiusVec = firstPos - center;
  double radius = radiusVec.mag();

  double circumference = 2 * PI * radius;
  double angleInc = 2 * PI * chordLen / circumference;

  double startAng;
  if (radiusVec(1) >= 0) {
    startAng = radiusVec.angleBetween(Vec2d(1,0));
  }
  else {
    startAng = 2 * PI - radiusVec.angleBetween(Vec2d(1,0));
  }

  positions.clear();
  positions.push_back(firstPos);
  for(double currInc = angleInc; currInc < PI; currInc += angleInc) {
    Vec2d pos1(center(0) + radius * cos(startAng + currInc),
               center(1) + radius * sin(startAng + currInc));
    
    positions.push_back(pos1);

    Vec2d pos2(center(0) + radius * cos(startAng - currInc),
               center(1) + radius * sin(startAng - currInc));

    positions.push_back(pos2);
  }
}

unsigned long gettime() {
  timeval t;
  gettimeofday(&t, NULL);
  return t.tv_sec * 1000000 + t.tv_usec;
}

int gobX(GameObj* gob) {
  return *gob->sod.x;
}

int gobY(GameObj* gob) {
  return *gob->sod.y;
}


bool damageTaken(int dir, uint4 dmg_vector) {
  uint4 mask = 1 << dir;
  return (mask & dmg_vector);
}
