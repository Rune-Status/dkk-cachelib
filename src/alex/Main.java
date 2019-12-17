package alex;

import java.util.Arrays;

import alex.cache.Cache;
import alex.cache.loaders.ItemDefinition;

public class Main {

	/*
	 * ----------\_/--------------
	 * ----------/-\--------------
	 * -------|-/@.@\-|----------
	 * ---------\___/-------------
	 * ALL CREDITS TO ALEX(DRAGONKK)
	 * CREATED DATA 18/04/2011
	 * @@alex_dkk@hotmail.com@@
	 * ----------------------------
	 * ----------------------------
	 * ----------------------------
	 */
	
	public static void main(String[] args) {
		
		Cache cache = new Cache("data/cache");
		Cache cache2 = new Cache("C:/Users/luismartins/Desktop/alex/Cache_Extractor/data");
		if(cache.getCacheFileManagers()[19].putAllContainers(cache2)) {
			System.out.println("Packed 643 items on 562 cache sucefully.");
			ItemDefinition test = new ItemDefinition(cache, 19000);
			System.out.println("Id "+test.id+", name: "+test.getName());
		}
		if(cache.getCacheFileManagers()[8].putAllContainers(cache2)) {
			System.out.println("Packed 643 models on 562 cache sucefully.");
		}
		System.out.println("UKEYS "+Arrays.toString(cache.generateUkeys()));
	}

}
