package Zako;

import java.util.Random;

import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.AbstractPlayer;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Board;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Action;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Puyo.PuyoDirection;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.PuyoPuyo;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Field;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Puyo;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.GameInfo;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.storage.PuyoType;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Puyo.PuyoNumber;
import sp.AbstractSamplePlayer;

 
public class zako_zako extends AbstractSamplePlayer {
	public int[][] current_map;
	public Field current_field;
	public Field enemy_field;
	public Board current_board;
	public Puyo current_puyo;
	public Puyo next_puyo;
	public Puyo nextnext_puyo;
	public int turn = 0;
	public int totalOjama;
	public int current_turn_ojama;
	public int max_combo;
	public int enemy_total_puyo;
	public int enemy_max_combo;
	public int[] result;
	public int beta;
	public static int prep_period = 30;
	public static int attack_period = 0;
	public static boolean reinforce = false;
	public static int game = 0;

	public zako_zako(String playerName) {
		super(playerName);
	}
	
	@Override
	public Action doMyTurn() {
		long start = System.currentTimeMillis();
		turn = getGameInfo().getTurn() + 1;
		current_board = getMyBoard();
		totalOjama = current_board.getTotalNumberOfOjama();
		current_turn_ojama = current_board.getCurrentTurnNumberOfOjama();
		current_field = current_board.getField();
		enemy_field = getEnemyBoard().getField();
		current_map = create_map(current_field);
		//System.out.println(turn);
		
		current_puyo = current_board.getCurrentPuyo();	
		next_puyo = current_board.getNextPuyo();
		nextnext_puyo = current_board.getNextNextPuyo();
		enemy_total_puyo = sum_puyo(enemy_field);
		max_combo = max_combo_num(current_field, current_puyo);
		enemy_max_combo = max_combo_num(enemy_field, current_puyo);
		//System.out.println("現盤面の準備連鎖数：" + max_combo);
		//System.out.println("Prep:"+prep_period);		
		//System.out.println("enemy盤面の準備連鎖数：" + enemy_max_combo);
		
		
		if(getGameInfo().getGameCount() != game) {
			prep_period = 30;
			attack_period = 0;
			reinforce = false;
			game += 1;
		}
		
		int[] ifprep = preparation(current_map, current_field, current_puyo, next_puyo, nextnext_puyo);
		current_puyo.setDirection(PuyoDirection.values()[ifprep[0]]);
		Field next_field = current_field.getNextField(current_puyo, ifprep[1]);
		int next_max_combo = max_combo_num(next_field, next_puyo);
	
		
		if(turn == 1) {
			result = ifprep;
			return new Action(PuyoDirection.values()[result[0]], result[1]);
			//return new Action(PuyoDirection.UP, 1);
		}
		
		
		if(sum_puyo(current_field) > 60) {
			beta = 2;
		}
		else beta = 4;
		
		
		if(totalOjama > 0) {
			if(current_turn_ojama > 0) {
				result = find_max_combo(current_map, current_field, current_puyo);
				return new Action(PuyoDirection.values()[result[0]], result[1]);
			}
			else {
				if(max_combo > next_max_combo) {
					result = find_max_combo(current_map, current_field, current_puyo);
				    return new Action(PuyoDirection.values()[result[0]], result[1]);
				}
				else {
					result = ifprep;
					prep_period -= 1;
					return new Action(PuyoDirection.values()[result[0]], result[1]);
				}
			}
		}

		
		if(max_combo >= beta) {
			if(max_combo > next_max_combo) {
				result = find_max_combo(current_map, current_field, current_puyo);
				prep_period = 30;
			    attack_period = 0;
			    return new Action(PuyoDirection.values()[result[0]], result[1]);
			}
			else {
				result = ifprep;
				prep_period -= 1;
				return new Action(PuyoDirection.values()[result[0]], result[1]);
			}
		}
		
		
		//reinforce cycle
		if((enemy_total_puyo > 45 && haveOjama(enemy_field) == true) || getEnemyBoard().getTotalNumberOfOjama() != 0) {
			if(max_combo > 1) {
				if(max_combo > next_max_combo) {
					result = find_max_combo(current_map, current_field, current_puyo);
				    return new Action(PuyoDirection.values()[result[0]], result[1]);
				}
				else {
					result = ifprep;
					return new Action(PuyoDirection.values()[result[0]], result[1]);
				}
			}
			else {
				result = ifprep;
				return new Action(PuyoDirection.values()[result[0]], result[1]);
			}
		}
		else {
			result = ifprep;
		}
		
	
		long end = System.currentTimeMillis();
		//System.out.println(end - start + "ms" + '\n');
		return new Action(PuyoDirection.values()[result[0]], result[1]);
	}
	
	
	//------map&neighbor----------------------
	public int[][] create_map(Field target_field) {
		int[][] map = new int[6][13];
		for(int i = 0; i < 6; i++) {
			int num_puyo = target_field.getTop(i);

			if(num_puyo == -1) continue;
			else {
				for(int j = 0; j <= num_puyo; j++) {
					switch (target_field.getPuyoType(i, j)) {
					case BLUE_PUYO:
						map[i][j] = 1;
						break;
						
					case GREEN_PUYO:
						map[i][j] = 2;
						break;
					
					case OJAMA_PUYO:
						map[i][j] = 3;
						break;
					
					case PURPLE_PUYO:
						map[i][j] = 4;
						break;
						
					case RED_PUYO:
						map[i][j] = 5;
						break;
					
					case YELLOW_PUYO:
						map[i][j] = 6;
						break;
					}
				}
			}
		}
 		return map;
	}
	

	public int[][] add_map(int[][] map, int x, int y, PuyoType type) {
 		switch (type) {
		case BLUE_PUYO:
			map[x][y] = 1;
			break;
			
		case GREEN_PUYO:
			map[x][y] = 2;
			break;
		
		case OJAMA_PUYO:
			map[x][y] = 3;
			break;
		
		case PURPLE_PUYO:
			map[x][y] = 4;
			break;
			
		case RED_PUYO:
			map[x][y] = 5;
			break;
		
		case YELLOW_PUYO:
			map[x][y] = 6;
			break;
 		}
 		return map;
 	}

	public void print_map(int[][] map) {
		for(int j = 12; j >=0; j--) {
			for(int i = 0; i < 6; i++) {
				System.out.print(map[i][j]);
				System.out.print(" ");
			}
			System.out.print('\n');
		}
		System.out.print('\n');
	}

	public int[][][][] create_neighbor(int[][] map) {
		int[][][][] neighbor = new int[6][13][4][2];
		
		for(int m = 0; m < 6; m++) {
			for(int n = 0; n < 13; n++) {
				for(int m1 = 0; m1 < 4; m1++) {
				    for(int n1 = 0; n1 < 2; n1++) {
				    	neighbor[m][n][m1][n1] = -1;
				    }
				}
			}
		}
		
		for(int i = 0; i < 6; i++) {
			for(int j = 0; j < 13; j++) {
				
				if(map[i][j] == 0) {
					continue;
				}
				if(map[i][j] != 0) {
					//UP
					if((j < 12) && (map[i][j+1] != 0) ) {
						neighbor[i][j][0][0] = i;
						neighbor[i][j][0][1] = j+1;
					}
					
					//RIGHT good
					if((i < 5) && (map[i+1][j] != 0) ) {
						neighbor[i][j][1][0] = i+1;
						neighbor[i][j][1][1] = j;
					}
					
					//DOWN good
					if((j > 0) && (map[i][j-1] != 0) ) {
						neighbor[i][j][2][0] = i;
						neighbor[i][j][2][1] = j-1;
					}
					
					//LEFT
					if((i > 0) && (map[i-1][j] != 0) ) {
					    neighbor[i][j][3][0] = i-1;
					    neighbor[i][j][3][1] = j;
					}
				}
			}
		}
		
		return neighbor;
	}

	public int[] find_puyo_coord(Puyo.PuyoDirection dir, int col, Field current_field) {
		int[] result = new int[4];
	    
	    switch(dir) {
	    case UP: 
	    	result[3] = current_field.getTop(col)+2;
	    	result[2] = col;
	    	result[1] = current_field.getTop(col)+1;
	    	result[0] = col;
	    	break;
	    	
	    case RIGHT:
	    	result[3] = current_field.getTop(col+1)+1;
	    	result[2] = col+1;
	    	result[1] = current_field.getTop(col)+1;
	    	result[0] = col;
	    	break;
	    	
	    case DOWN:
	    	result[3] = current_field.getTop(col)+1;
	    	result[2] = col;
	    	result[1] = current_field.getTop(col)+2;
	    	result[0] = col;
	    	break;
	    	
	    case LEFT:
	    	result[3] = current_field.getTop(col-1)+1;
	    	result[2] = col-1;
	    	result[1] = current_field.getTop(col)+1;
	    	result[0] = col;
	    	break;
	    }
	    return result;
	}
	
	//-------link_search---------------------------
	public int map_search(int x, int y, int[][][][] neighbor, int[][] map) {
		String[][] traved = new String[6][13];
		return start(x, y, traved, neighbor, map) + 1;
	}
	
	public int start(int x, int y, String[][] traved, int[][][][] neighbor, int[][] map) {
    	int all_link = 0;
    	int[] start_point = {x, y};
        
    	for(int[] p: neighbor[x][y]) {
    		if(p[0] != -1 && p[1] != -1) {
    			int[] neightbor_point = p;
    			if(traved[neightbor_point[0]][neightbor_point[1]] != null) {
    				continue;
    			}
    			else {
    				traved[x][y] = "X";
                    traved[neightbor_point[0]][neightbor_point[1]] = "X";
    				all_link += trace(start_point, neightbor_point, traved, neighbor, map);
    			}
    		}
    		else continue;
    	}
    	return all_link;
    }
	
	public int trace(int[] point1, int[] point2, String[][] traved, int[][][][] neighbor, int[][] map) {
    	if(map[point1[0]][point1[1]] == map[point2[0]][point2[1]]) {
    		return start(point2[0], point2[1], traved, neighbor, map) + 1;
    	}
    	else return 0;
    }
	
	
	
	//------solver & preparation--------------
	public int[] find_max_combo(int[][] map, Field cur_field, Puyo cur_puyo) {
		int[] result = new int[2];
		int max_combo = 0; 
		int combo;
		
		for(int d = 0; d < 4; d++){
			for(int col = 0; col < 6; col++) {
				//Check whether can be put
				PuyoDirection dir = PuyoDirection.values()[d];
				if(!cur_field.isEnable(dir, col)) continue;
				
				//Check if dead
				cur_puyo.setDirection(dir);
				Field next_field = cur_field.getNextField(cur_puyo, col);
				if(next_field.isDead()) continue;
				
				combo = return_combo(cur_field, dir, col, cur_puyo);
				
				//因为是大于等于，result保留了最后一个的坐标
				if(combo >= max_combo) {
					max_combo = combo;
					result[0] = d;
					result[1] = col;
				}
			}   		
		}
		
		//if(max_combo <= 0) result = preparation(map, cur_field, cur_puyo, next_puyo, nextnext);
		return result;
	}
	
	public int max_combo_num(Field field, Puyo puyo) {
		int max_combo = 0; 
		int combo;
		
		for(int d = 0; d < 4; d++){
			for(int col = 0; col < 6; col++) {
				//Check whether can be put
				PuyoDirection dir = PuyoDirection.values()[d];
				if(!field.isEnable(dir, col)) continue;
				
				//Check if dead
				puyo.setDirection(dir);
				Field next_field = field.getNextField(puyo, col);
				puyo.setDirection(null);
				if(next_field.isDead()) continue;
				
				combo = return_combo(field, dir, col, puyo);
				//System.out.println(combo);				
				if(combo >= max_combo) {
					max_combo = combo;
				}
			}   		
		}
		return max_combo;
	}
	
    public int return_combo(Field target_field, PuyoDirection dir, int col, Puyo puyo) {
    	int combo = 0;
    	puyo.setDirection(dir);
    	int[][] final_map = create_map(target_field.getNextField(puyo, col));
    	
    	int[][] map = create_map(target_field);
	    int[] puyo_coord = find_puyo_coord(dir, col, target_field);
		map = add_map(map, puyo_coord[0], puyo_coord[1], puyo.getPuyoType(PuyoNumber.FIRST));
		map = add_map(map, puyo_coord[2], puyo_coord[3], puyo.getPuyoType(PuyoNumber.SECOND));
    	int[][][][] neighbor = create_neighbor(map);
    	
		if(puyo.getPuyoType(PuyoNumber.FIRST) != puyo.getPuyoType(PuyoNumber.SECOND)) {
			int link_jiku = map_search(puyo_coord[0], puyo_coord[1], neighbor, map);
			int link_kumi = map_search(puyo_coord[2], puyo_coord[3], neighbor, map);
			
			if((link_jiku >= 4) && (link_kumi >= 4)) {
				combo += 1;
				put_zero(map, puyo_coord[0], puyo_coord[1]);
				put_zero(map, puyo_coord[2], puyo_coord[3]);
			}
			if(link_jiku >= 4 && link_kumi < 4) {
				combo += 1;
				put_zero(map, puyo_coord[0], puyo_coord[1]);
			}
			if(link_kumi >= 4 && link_jiku < 4) {
				combo += 1;
				put_zero(map, puyo_coord[2], puyo_coord[3]);
			}
			
			map_update(map);
		}
		else {
			int link = map_search(puyo_coord[0], puyo_coord[1], neighbor, map);
			
			if(link >= 4) {
				combo += 1;
				put_zero(map, puyo_coord[0], puyo_coord[1]);
			}
			
			map_update(map);
		}
		
		//まだ連鎖があるかをチェック
		for(int m = 0; m < 7; m++) {
			if(mapEquals(map, final_map)) break;
			boolean ifcombo = false;
			neighbor = create_neighbor(map);
			
			for(int j = 12; j >= 0; j--) {
				for(int i = 0; i < 6; i++) {
					if(map[i][j] == 0) continue;
					else {
						int link = map_search(i, j, neighbor, map);
						if(link >= 4) {
							ifcombo = true;
							put_zero(map, i, j);
						}
						else continue;
					}
				}
			}
			if(ifcombo) combo += 1;
			map_update(map);
		}
    	return combo;
    }

	public int[] preparation(int [][] map, Field field, Puyo cur_puyo, Puyo next_puyo, Puyo nextnext_puyo) {
		int[] result = new int[3];
		//int[][][][] neighbor;
		double max_reward = 0;
		//double bonus = 1;
		Field after_action1;
		Field after_action2;
		Field after_action3;
		int[][] map1;
		int[][] map2;
		int[][] map3;
		int[][][][] neighbor1;
		int[][][][] neighbor2;
		int[][][][] neighbor3;
	    double reward1;
		double reward2;
		double reward3;
		
		
		//将来3個のぷよの情報を使う
		for(int d1 = 0; d1 < 4; d1++) {
			for(int col1 = 0; col1 < 6; col1++) {
				PuyoDirection dir1 = PuyoDirection.values()[d1];
				if(!field.isEnable(dir1, col1)) continue;
				cur_puyo.setDirection(dir1);
				after_action1 = field.getNextField(cur_puyo, col1);
				if(after_action1.isDead()) continue;	
				
				int[] puyo_coord = find_puyo_coord(dir1, col1, field);
				map1 = create_map(field);
				map1 = add_map(map1, puyo_coord[0], puyo_coord[1], cur_puyo.getPuyoType(PuyoNumber.FIRST));
			    map1 = add_map(map1, puyo_coord[2], puyo_coord[3], cur_puyo.getPuyoType(PuyoNumber.SECOND));
			    neighbor1 = create_neighbor(map1);
			    
			    int link_jiku1 = map_search(puyo_coord[0], puyo_coord[1], neighbor1, map1);
		    	int link_kumi1 = map_search(puyo_coord[2], puyo_coord[3], neighbor1, map1);
		    	if(link_jiku1 >= 4 || link_kumi1 >= 4) continue;
		    	else {
		    		double bonus1 = 1;
		    		if(col1 == 0 || col1 == 5) bonus1 = 0.9;
		    		//if((col1 == 1 && d1 == 3) || (col1 == 4 && d1 == 1)) bonus1 = 0.9;
		    		//if((col1 == 1 && d1 == 3) || (col1 == 4 && d1 == 1) || (col1 == 0 && d1 == 1) || (col1 == 5 && d1 == 3)) bonus1 = 0.95;
		    		reward1 = (Math.pow(5, link_jiku1) + Math.pow(5, link_kumi1)) * bonus1;
		    	}
				
		    	
				for(int d2 = 0; d2 < 4; d2++) {
					for(int col2 = 0; col2 < 6; col2++) {
						PuyoDirection dir2 = PuyoDirection.values()[d2];
						if(!after_action1.isEnable(dir2, col2)) continue;
						next_puyo.setDirection(dir2);
						after_action2 = after_action1.getNextField(next_puyo, col2);
						if(after_action2.isDead()) continue;
						
						int[] puyo_coord2 = find_puyo_coord(dir2, col2, after_action1);
						map2 = create_map(after_action1);
						map2 = add_map(map2, puyo_coord2[0], puyo_coord2[1], next_puyo.getPuyoType(PuyoNumber.FIRST));
					    map2 = add_map(map2, puyo_coord2[2], puyo_coord2[3], next_puyo.getPuyoType(PuyoNumber.SECOND));
					    neighbor2 = create_neighbor(map2);
					    
					    int link_jiku2 = map_search(puyo_coord2[0], puyo_coord2[1], neighbor2, map2);
				    	int link_kumi2 = map_search(puyo_coord2[2], puyo_coord2[3], neighbor2, map2);
				    	if(link_jiku2 >= 4 || link_kumi2 >= 4) continue;
				    	else {
				    		double bonus2 = 1;
				    		if(col2 == 0 || col2 == 5) bonus2 = 0.9;
				    		//if((col2 == 1 && d2 == 3) || (col2 == 4 && d2 == 1)) bonus2 = 0.9;
				    		//if((col2 == 1 && d2 == 3) || (col2 == 4 && d2 == 1) || (col2 == 0 && d2 == 1) || (col2 == 5 && d2 == 3)) bonus2 = 0.95;
				    		reward2 = (Math.pow(5, link_jiku2) + Math.pow(5, link_kumi2)) * bonus2;
				    	}
		
						
						for(int d3 = 0; d3 < 4; d3++) {
							for(int col3 = 0; col3 < 6; col3++) {
								PuyoDirection dir3 = PuyoDirection.values()[d3];
								if(!after_action2.isEnable(dir3, col3)) continue;
								nextnext_puyo.setDirection(dir3);
								after_action3 = after_action2.getNextField(nextnext_puyo, col3);
								if(after_action3.isDead()) continue;
								
								int combo = return_combo(after_action2, dir3, col3, nextnext_puyo);
								reward3 = reward1 + reward2 + 70 * combo;
								if(reward3 >= max_reward) {
									max_reward = reward3;
									result[0] = d1;
									result[1] = col1;
								}
								/*
								int[] puyo_coord3 = find_puyo_coord(dir3, col3, after_action2);
								map3 = create_map(after_action2);
								add_map(map3, puyo_coord3[0], puyo_coord3[1], next_puyo.getPuyoType(PuyoNumber.FIRST));
							    add_map(map3, puyo_coord3[2], puyo_coord3[3], next_puyo.getPuyoType(PuyoNumber.SECOND));
							    neighbor3 = create_neighbor(map3);
							    
							    if(next_puyo.getPuyoType(PuyoNumber.FIRST) != next_puyo.getPuyoType(PuyoNumber.SECOND)) {
							    	int link_jiku = map_search(puyo_coord3[0], puyo_coord3[1], neighbor3, map3);
							    	int link_kumi = map_search(puyo_coord3[2], puyo_coord3[3], neighbor3, map3);
							    	if(link_jiku >= 4 || link_kumi >= 4) continue;
							    	else reward3 = Math.pow(5, link_jiku) + Math.pow(5, link_kumi);
							    }
							    else {
							    	int link = map_search(puyo_coord3[0], puyo_coord3[1], neighbor3, map3);
							    	if(link >= 4) continue;
							    	else reward3 = Math.pow(5, link);
							    }
							    double reward_all = 0.2 * reward1 + 0.3 * reward2 + 0.5 * reward3;
								if(reward_all >= max_reward) {
									max_reward = reward_all;
									result[0] = d1;
									result[1] = col1;
									*/
								
							}
						}
					}
				}
			}
		}
		return result;
		/*
		*/
	}
	
	

	//------------tools----------------
	//------tools-----------------
 	public int sum_puyo(Field field) {
		int sum = 0;
		for(int i = 0; i < 6; i++) {
			sum = sum + field.getTop(i) + 1;
		}
		return sum;
	}
	
 	
 	public void put_zero(int[][] map, int x, int y){
 		int color = map[x][y];
 		map[x][y] = 0;
 		
 		//お邪魔ぷよを消す
 		//UP
 		if(y < 12 && map[x][y+1] == 3) map[x][y+1] = 0;;
 		//RIGHT
 		if(x < 5 && map[x+1][y] == 3) map[x+1][y] = 0;
 		//DOWN
 		if(y > 0 && map[x][y-1] == 3) map[x][y-1] = 0;
 		//LEFT
 		if(x > 0 && map[x-1][y] == 3) map[x-1][y] = 0;
 		
 		//UP
 		if(y < 12 && map[x][y+1] == color) put_zero(map, x, y+1);
 		//RIGHT
 		if(x < 5 && map[x+1][y] == color) put_zero(map, x+1, y);
 		//DOWN
 		if(y > 0 && map[x][y-1] == color) put_zero(map, x, y-1);
 		//LEFT
 		if(x > 0 && map[x-1][y] == color) put_zero(map, x-1, y);
 		
 	}
 	
 	
    public void map_update(int[][] map){
 		for(int i = 0; i < 6; i++) {
 			int[] col = new int[13];
 			int index = 0;
 			
 			for(int j = 0; j < 13; j++) {
 				if(map[i][j] == 0) continue;
 				else {
 					col[index] = map[i][j];
 					index += 1;
 				}
 			}
 			
 			for(int j = 0; j < 13; j++) {
 				map[i][j] = col[j];
 			}
 		}
 	}
    
 	
 	public boolean mapEquals(int[][] a1, int[][] a2) {
 		for(int i = 0; i < 6; i++) {
 			for(int j = 0; j < 13; j++) {
 				if(a1[i][j] != a2[i][j]) {
 					return false;
 				}
 			}
 		}
 		return true;
 	}
 	
 	public boolean haveOjama(Field field) {
 		for(int i = 0; i < 6; i++) {
			int num_puyo = field.getTop(i);
			if(num_puyo == -1) continue;
			else {
				for(int j = 0; j <= num_puyo; j++) {
					if(field.getPuyoType(i, j) == PuyoType.OJAMA_PUYO) {
					    return true;
					}
				}
			}
		}	
 		return false;
 	}
 
	
 	
 	public static void main(String args[]) {
		AbstractPlayer player1 = new zako_zako("zakozako1");

		PuyoPuyo puyopuyo = new PuyoPuyo(player1);
		puyopuyo.puyoPuyo();
	}
}
