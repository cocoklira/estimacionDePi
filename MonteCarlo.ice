module MonteCarlo{

    interface Worker {

        void generatePoints(long numPoints);
        long getPointsInside();
        long getTotalPoints();
    };



    interface Master {

        void calculatePI(long totalPoints, int numWorkers);
        double getPIValue();
        string getCalculationStatus();
    };
};