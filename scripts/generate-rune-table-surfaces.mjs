import fs from 'node:fs';
import {Pixels,png} from './generate-prototype-textures.mjs';
const base='src/main/resources/assets/modularmagic';
const research=new Pixels().rect(0,0,16,16,'#503538').rect(1,1,14,14,'#a77856').rect(2,2,12,12,'#e4d7b4').rect(3,3,4,1,'#8f8373').rect(3,5,3,1,'#8f8373').rect(3,7,2,1,'#8f8373').rect(10,11,3,1,'#8f8373').rect(10,13,2,1,'#8f8373');
research.rect(7,3,6,7,'#926953').rect(6,4,8,5,'#926953').rect(8,4,4,5,'#397e7b').rect(7,5,6,3,'#397e7b').rect(8,5,3,2,'#8bcfc0').rect(8,5,1,2,'#ddf9d6').line(6,10,4,13,'#75574b').line(5,10,3,13,'#c99c65');
const carving=new Pixels().rect(0,0,16,16,'#503538').rect(1,1,14,14,'#a77856').rect(2,2,12,12,'#243346').rect(3,3,10,1,'#586c7c').rect(3,12,10,1,'#121e30').line(4,8,7,5,'#b9e6ec').line(7,5,10,8,'#b9e6ec').line(10,8,7,11,'#578fa9').line(7,11,4,8,'#578fa9').rect(6,7,3,3,'#53b7d1').rect(7,7,1,2,'#dafbfa').line(10,12,13,5,'#edcf98').line(11,12,14,5,'#b78c63').line(10,13,11,11,'#c1ced8').rect(3,4,2,1,'#bd9c6e');
for(const[id,p]of[['rune_research_table',research],['rune_carving_table',carving]]){
 fs.writeFileSync(`${base}/textures/block/${id}_surface.png`,png(p));
 const file=`${base}/models/block/${id}.json`,model=JSON.parse(fs.readFileSync(file,'utf8'));model.textures.top=`modularmagic:block/${id}_surface`;fs.writeFileSync(file,JSON.stringify(model,null,2)+'\n');
}
