DROP FUNCTION IF EXISTS calculerDistanceKm;
#--
CREATE FUNCTION calculerDistanceKm(lt1 double, lg1 double, lt2 double, lg2 double)
  RETURNS double
BEGIN
  DECLARE distance double;
  SELECT round( (6371 * acos( cos(radians(lt1)) * cos(radians(lt2)) * cos(radians(lg2) - radians(lg1)) + sin(radians(lt1)) * sin(radians(lt2)))), 3)
    INTO distance;
  RETURN distance;
END;
#--
DROP PROCEDURE IF EXISTS listerLieuxProchesPosition;
#--
CREATE PROCEDURE listerLieuxProchesPosition(lt double, lg double, offset integer, nb integer)
  BEGIN
    call listerLieuxProchesPositionDansRayon(lt, lg, offset, nb, 50);
  END;
#--
DROP PROCEDURE IF EXISTS listerLieuxProchesPositionDansRayon;
#--
CREATE PROCEDURE listerLieuxProchesPositionDansRayon(lt double, lg double, offset integer, nb integer, rayonMax double)
  BEGIN
    SELECT id, calculerDistanceKm(latitude, longitude, lt, lg) as distance
    FROM lieux
    HAVING distance <= rayonMax
    ORDER BY DISTANCE ASC
    LIMIT offset, nb;
  END;
#--