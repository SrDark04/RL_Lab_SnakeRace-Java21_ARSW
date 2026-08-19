/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.eci.primefinder;

import java.util.*;

/**
 *
 */
public class Control extends Thread {

    private final static int NTHREADS = 3;
    private final static int MAXVALUE = 30000000;
    private final static int TMILISECONDS = 1000;

    private final int NDATA = MAXVALUE / NTHREADS;

    private PrimeFinderThread pft[];
    private PausedControl control = new PausedControl();

    private Control() {
        super();
        this.pft = new PrimeFinderThread[NTHREADS];

        int i;
        for (i = 0; i < NTHREADS - 1; i++) {
            PrimeFinderThread elem = new PrimeFinderThread(i * NDATA, (i + 1) * NDATA, control);
            pft[i] = elem;
        }
        pft[i] = new PrimeFinderThread(i * NDATA, MAXVALUE + 1, control);
    }

    public static Control newControl() {
        return new Control();
    }

    @Override
    public void run() {
        for (int i = 0; i < NTHREADS; i++) {
            pft[i].start();
        }
        while (threadsAlive()) {
            try (Scanner scanner = new Scanner(System.in)) {
                Thread.sleep(TMILISECONDS);

                if (!threadsAlive()) {
                    break;
                }

                control.pauseAllThreads();
                System.out.println("========================= PAUSED ===========================");
                int cont = 0;
                for (PrimeFinderThread p : pft) {
                    cont += p.getPrimes().size();
                }
                System.out.println("Total primer number found =" + cont);
                System.out.println("Press enter to continue...");
                scanner.nextLine();
                // System.out.println("========================= RESUMED
                // ===========================");
                control.resumeAllThreads();
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private boolean threadsAlive() {
        for (PrimeFinderThread p : pft) {
            if (p.isAlive()) {
                return true;
            }
        }
        return false;
    }

}
