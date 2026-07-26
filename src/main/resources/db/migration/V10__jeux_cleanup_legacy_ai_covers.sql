UPDATE jeux
SET cover_image_url = NULL
WHERE cover_image_url IS NOT NULL
  AND (
    cover_image_url ILIKE 'https://image.pollinations.ai/%'
    OR cover_image_url ILIKE 'data:image/svg+xml%'
  );

